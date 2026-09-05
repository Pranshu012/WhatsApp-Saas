package com.example.wasaas.lead;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.contact.Conversation;
import com.example.wasaas.contact.ConversationMessage;
import com.example.wasaas.contact.ConversationMessageRepository;
import com.example.wasaas.contact.ConversationRepository;
import com.example.wasaas.ledger.PhonePrivacyUtils;
import com.example.wasaas.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leadRepository;
    private final LeadQualificationRuleRepository ruleRepository;
    private final ContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    public LeadService(LeadRepository leadRepository,
                       LeadQualificationRuleRepository ruleRepository,
                       ContactRepository contactRepository,
                       ConversationRepository conversationRepository,
                       ConversationMessageRepository conversationMessageRepository) {
        this.leadRepository = leadRepository;
        this.ruleRepository = ruleRepository;
        this.contactRepository = contactRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @Transactional
    public Lead getOrCreateLead(UUID tenantId, UUID contactId) {
        return leadRepository.findByTenantIdAndContactId(tenantId, contactId)
                .orElseGet(() -> {
                    Lead lead = new Lead(tenantId, contactId);
                    enrichLeadFromPastMessages(lead, tenantId, contactId);
                    return leadRepository.saveAndFlush(lead);
                });
    }

    @Transactional
    public void processCustomerMessage(UUID tenantId, UUID contactId, String textContent) {
        if (textContent == null || textContent.isBlank()) return;

        Lead lead = getOrCreateLead(tenantId, contactId);
        lead.setLastInteractionAt(Instant.now());

        if (lead.getStage() == LeadStage.NEW) {
            lead.setStage(LeadStage.IN_DISCUSSION);
        }

        // Evaluate Tenant Qualification Rules
        List<LeadQualificationRule> rules = ruleRepository.findAllByTenantIdAndActiveTrue(tenantId);
        if (rules.isEmpty()) {
            rules = List.of(new LeadQualificationRule(
                    tenantId,
                    "High Intent / Pricing Inquiry",
                    "price,cost,rate,charges,budget,quote,quotation,buy,purchase,book,order,deal,plan,features",
                    25
            ));
        }

        String lowerText = textContent.toLowerCase();
        int totalBoost = 0;
        for (LeadQualificationRule rule : rules) {
            String[] keywords = rule.getMatchKeywords().split(",");
            for (String kw : keywords) {
                String trimmed = kw.trim().toLowerCase();
                if (!trimmed.isEmpty() && lowerText.contains(trimmed)) {
                    totalBoost += rule.getScoreBoost();
                    break;
                }
            }
        }

        if (totalBoost > 0) {
            int newScore = Math.min(100, lead.getScore() + totalBoost);
            lead.setScore(newScore);
            log.info("Boosted lead [{}] score by [{}] to [{}] for tenant [{}]",
                    lead.getId(), totalBoost, newScore, tenantId);
        }

        // Auto-update requirement summary if empty or customer is describing needs
        if (lead.getRequirementSummary() == null || lead.getRequirementSummary().isBlank() || totalBoost > 0) {
            String snippet = textContent.trim();
            if (snippet.length() > 90) {
                snippet = snippet.substring(0, 90) + "...";
            }
            lead.setRequirementSummary(snippet);
        }

        leadRepository.saveAndFlush(lead);
    }

    @Transactional
    public List<LeadDto> listLeads() {
        UUID tenantId = TenantContext.require();
        List<Lead> leads = leadRepository.findAllByTenantIdOrderByLastInteractionAtDesc(tenantId);

        // Also backfill any contacts that don't have leads yet
        List<Contact> allContacts = contactRepository.findAllByTenantId(tenantId);
        Set<UUID> existingLeadContactIds = new HashSet<>();
        for (Lead l : leads) {
            existingLeadContactIds.add(l.getContactId());
        }

        for (Contact c : allContacts) {
            if (!existingLeadContactIds.contains(c.getId())) {
                Optional<Lead> existing = leadRepository.findByTenantIdAndContactId(tenantId, c.getId());
                if (existing.isPresent()) {
                    leads.add(existing.get());
                    existingLeadContactIds.add(c.getId());
                } else {
                    try {
                        Lead newLead = new Lead(tenantId, c.getId());
                        enrichLeadFromPastMessages(newLead, tenantId, c.getId());
                        Lead saved = leadRepository.saveAndFlush(newLead);
                        leads.add(saved);
                        existingLeadContactIds.add(c.getId());
                    } catch (Exception e) {
                        leadRepository.findByTenantIdAndContactId(tenantId, c.getId()).ifPresent(leads::add);
                    }
                }
            }
        }

        List<LeadDto> dtos = new ArrayList<>();
        for (Lead l : leads) {
            Contact contact = contactRepository.findById(l.getContactId()).orElse(null);
            dtos.add(toDto(l, contact));
        }

        return dtos;
    }

    @Transactional
    public LeadDto createManualLead(String name, String phoneE164, LeadStage stage, BigDecimal dealValue, String notes) {
        UUID tenantId = TenantContext.require();
        String formattedPhone = phoneE164.trim();
        if (!formattedPhone.startsWith("+")) {
            formattedPhone = "+" + formattedPhone;
        }

        String phoneHash = PhonePrivacyUtils.hashPhoneNumber(formattedPhone);
        String finalPhone = formattedPhone;
        Contact contact = contactRepository.findByTenantIdAndPhoneE164(tenantId, formattedPhone)
                .orElseGet(() -> {
                    Contact c = new Contact(tenantId, finalPhone, phoneHash, name);
                    return contactRepository.saveAndFlush(c);
                });

        if (name != null && !name.isBlank() && (contact.getDisplayName() == null || contact.getDisplayName().isBlank())) {
            contact.updateActivity(name, Instant.now());
            contactRepository.saveAndFlush(contact);
        }

        Lead lead = leadRepository.findByTenantIdAndContactId(tenantId, contact.getId())
                .orElseGet(() -> new Lead(tenantId, contact.getId()));

        if (stage != null) lead.setStage(stage);
        if (dealValue != null) lead.setDealValue(dealValue);
        if (notes != null) lead.setNotes(notes.trim());
        lead.setLastInteractionAt(Instant.now());

        Lead saved = leadRepository.saveAndFlush(lead);
        return toDto(saved, contact);
    }

    @Transactional
    public LeadDto updateStage(UUID leadId, LeadStage newStage) {
        UUID tenantId = TenantContext.require();
        Lead lead = leadRepository.findByTenantIdAndId(tenantId, leadId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        lead.setStage(newStage);
        if (newStage == LeadStage.WON) {
            lead.setScore(100);
        } else if (newStage == LeadStage.LOST) {
            lead.setScore(10);
        }
        lead.setLastInteractionAt(Instant.now());

        Lead saved = leadRepository.saveAndFlush(lead);
        Contact contact = contactRepository.findById(saved.getContactId()).orElse(null);
        return toDto(saved, contact);
    }

    @Transactional
    public LeadDto updateDetails(UUID leadId, LeadStage stage, BigDecimal dealValue, String notes) {
        UUID tenantId = TenantContext.require();
        Lead lead = leadRepository.findByTenantIdAndId(tenantId, leadId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        if (stage != null) {
            lead.setStage(stage);
            if (stage == LeadStage.WON) {
                lead.setScore(100);
            } else if (stage == LeadStage.LOST) {
                lead.setScore(10);
            }
        }
        if (dealValue != null) {
            lead.setDealValue(dealValue);
        }
        if (notes != null) {
            lead.setNotes(notes.trim());
        }
        lead.setLastInteractionAt(Instant.now());

        Lead saved = leadRepository.saveAndFlush(lead);
        Contact contact = contactRepository.findById(saved.getContactId()).orElse(null);
        return toDto(saved, contact);
    }

    @Transactional
    public LeadDto rescoreLead(UUID leadId) {
        UUID tenantId = TenantContext.require();
        Lead lead = leadRepository.findByTenantIdAndId(tenantId, leadId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        enrichLeadFromPastMessages(lead, tenantId, lead.getContactId());
        Lead saved = leadRepository.saveAndFlush(lead);
        Contact contact = contactRepository.findById(saved.getContactId()).orElse(null);
        return toDto(saved, contact);
    }

    @Transactional(readOnly = true)
    public LeadAnalyticsDto getAnalytics() {
        UUID tenantId = TenantContext.require();
        List<Lead> leads = leadRepository.findAllByTenantIdOrderByLastInteractionAtDesc(tenantId);

        long totalLeads = leads.size();
        long newLeads = 0;
        long inDiscussion = 0;
        long proposalSent = 0;
        long won = 0;
        long lost = 0;
        long hotLeads = 0;
        BigDecimal totalPipelineValue = BigDecimal.ZERO;
        BigDecimal wonValue = BigDecimal.ZERO;

        for (Lead l : leads) {
            if (l.getStage() == LeadStage.NEW) newLeads++;
            else if (l.getStage() == LeadStage.IN_DISCUSSION) inDiscussion++;
            else if (l.getStage() == LeadStage.PROPOSAL_SENT) proposalSent++;
            else if (l.getStage() == LeadStage.WON) {
                won++;
                if (l.getDealValue() != null) {
                    wonValue = wonValue.add(l.getDealValue());
                }
            } else if (l.getStage() == LeadStage.LOST) lost++;

            if (l.getTemperature() == LeadTemperature.HOT) hotLeads++;
            if (l.getStage() != LeadStage.LOST && l.getDealValue() != null) {
                totalPipelineValue = totalPipelineValue.add(l.getDealValue());
            }
        }

        double conversionRate = totalLeads > 0
                ? BigDecimal.valueOf((double) won / totalLeads * 100)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue()
                : 0.0;

        return new LeadAnalyticsDto(
                totalLeads,
                newLeads,
                inDiscussion,
                proposalSent,
                won,
                lost,
                hotLeads,
                totalPipelineValue,
                wonValue,
                conversionRate
        );
    }

    // Qualification Rules CRUD
    @Transactional(readOnly = true)
    public List<LeadQualificationRule> listRules() {
        UUID tenantId = TenantContext.require();
        List<LeadQualificationRule> rules = ruleRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        if (rules.isEmpty()) {
            LeadQualificationRule defaultRule = new LeadQualificationRule(
                    tenantId,
                    "High Intent & Pricing Inquiry",
                    "price,cost,rate,charges,budget,quote,quotation,buy,purchase,book,order,deal,plan",
                    25
            );
            ruleRepository.save(defaultRule);
            return List.of(defaultRule);
        }
        return rules;
    }

    @Transactional
    public LeadQualificationRule createRule(String ruleName, String matchKeywords, int scoreBoost) {
        UUID tenantId = TenantContext.require();
        LeadQualificationRule rule = new LeadQualificationRule(
                tenantId,
                ruleName.trim(),
                matchKeywords.trim(),
                scoreBoost > 0 ? scoreBoost : 20
        );
        return ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        UUID tenantId = TenantContext.require();
        LeadQualificationRule rule = ruleRepository.findByTenantIdAndId(tenantId, ruleId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Rule not found: " + ruleId));
        ruleRepository.delete(rule);
    }

    private void enrichLeadFromPastMessages(Lead lead, UUID tenantId, UUID contactId) {
        try {
            List<Conversation> conversations = conversationRepository.findAllByTenantIdAndContactId(tenantId, contactId);
            if (conversations.isEmpty()) return;

            List<LeadQualificationRule> rules = ruleRepository.findAllByTenantIdAndActiveTrue(tenantId);
            if (rules.isEmpty()) {
                rules = List.of(new LeadQualificationRule(
                        tenantId,
                        "High Intent & Pricing Inquiry",
                        "price,cost,rate,charges,budget,quote,quotation,buy,purchase,book,order,deal,plan,services",
                        25
                ));
            }

            String latestCustomerText = null;
            Instant latestMessageAt = null;
            int boost = 0;
            int customerMsgCount = 0;

            for (Conversation conv : conversations) {
                List<ConversationMessage> msgs = conversationMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(conv.getId());
                for (ConversationMessage msg : msgs) {
                    if ("CUSTOMER".equalsIgnoreCase(msg.getSenderType())) {
                        customerMsgCount++;
                        latestCustomerText = msg.getTextContent();
                        latestMessageAt = msg.getCreatedAt();
                        String lower = msg.getTextContent().toLowerCase();
                        for (LeadQualificationRule rule : rules) {
                            for (String kw : rule.getMatchKeywords().split(",")) {
                                String trimmed = kw.trim().toLowerCase();
                                if (!trimmed.isEmpty() && lower.contains(trimmed)) {
                                    boost += rule.getScoreBoost();
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (latestCustomerText != null) {
                if (lead.getRequirementSummary() == null || lead.getRequirementSummary().isBlank()) {
                    String snippet = latestCustomerText.trim();
                    if (snippet.length() > 90) {
                        snippet = snippet.substring(0, 90) + "...";
                    }
                    lead.setRequirementSummary(snippet);
                }
                if (lead.getStage() == LeadStage.NEW && customerMsgCount > 0) {
                    lead.setStage(LeadStage.IN_DISCUSSION);
                }
            }

            if (latestMessageAt != null) {
                lead.setLastInteractionAt(latestMessageAt);
            }

            int finalScore = Math.min(100, Math.max(lead.getScore(), 50 + boost + (customerMsgCount * 5)));
            lead.setScore(finalScore);
        } catch (Exception e) {
            log.warn("Could not enrich lead [{}] from conversation messages: {}", lead.getId(), e.getMessage());
        }
    }

    private LeadDto toDto(Lead l, Contact contact) {
        return new LeadDto(
                l.getId(),
                l.getContactId(),
                contact != null ? contact.getDisplayName() : null,
                contact != null ? contact.getPhoneE164() : null,
                l.getStage(),
                l.getTemperature(),
                l.getScore(),
                l.getDealValue(),
                l.getRequirementSummary(),
                l.getNotes(),
                l.getLastInteractionAt(),
                l.getCreatedAt()
        );
    }

    public record LeadDto(
            UUID id,
            UUID contactId,
            String contactName,
            String phoneE164,
            LeadStage stage,
            LeadTemperature temperature,
            int score,
            BigDecimal dealValue,
            String requirementSummary,
            String notes,
            Instant lastInteractionAt,
            Instant createdAt
    ) {}

    public record LeadAnalyticsDto(
            long totalLeads,
            long newLeads,
            long inDiscussion,
            long proposalSent,
            long won,
            long lost,
            long hotLeads,
            BigDecimal totalPipelineValue,
            BigDecimal wonValue,
            double conversionRatePercent
    ) {}
}
