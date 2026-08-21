package com.example.wasaas.scheduling;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.contact.Contact;
import com.example.wasaas.contact.ContactRepository;
import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.template.TemplateCategory;
import com.example.wasaas.template.TemplateStatus;
import com.example.wasaas.template.WhatsAppTemplate;
import com.example.wasaas.template.WhatsAppTemplateRepository;
import com.example.wasaas.tenant.RegistrationCommand;
import com.example.wasaas.tenant.TenantRepository;
import com.example.wasaas.tenant.TenantService;
import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.whatsapp.SaveWhatsAppAccountCommand;
import com.example.wasaas.whatsapp.WhatsAppAccount;
import com.example.wasaas.whatsapp.WhatsAppAccountService;
import com.example.wasaas.whatsapp.client.TemplateComponent;
import com.example.wasaas.whatsapp.client.TemplateParameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000",
    "app.jobs.batch-size=10",
    "app.jobs.lock-timeout-secs=300"
})
@ActiveProfiles({"local", "worker"})
public class SchedulingServiceTest {

    @Autowired private SchedulingService schedulingService;
    @Autowired private ScheduledMessageRepository scheduledMessageRepository;
    @Autowired private EnqueueDueScheduledMessagesHandler enqueueHandler;
    @Autowired private WhatsAppTemplateRepository templateRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private WhatsAppAccountService accountService;
    @Autowired private TenantService tenantService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID tenantAId;
    private WhatsAppAccount accountA;
    private Contact contactA;
    private WhatsAppTemplate templateA;

    @BeforeEach
    void setup() {
        cleanup();

        tenantService.registerTenant(new RegistrationCommand(
                "Scheduling Store A",
                "scheduling-store-a",
                "Schedule Admin",
                "admin.sched@example.com",
                "Password123!"
        ));
        tenantAId = tenantRepository.findBySlug("scheduling-store-a").orElseThrow().getId();
        TenantContext.set(tenantAId);

        accountA = accountService.saveOrUpdateAccount(new SaveWhatsAppAccountCommand(
                "waba_sched_1001",
                "phone_sched_1001",
                "+1 555-6666",
                "Scheduler Bot",
                "GREEN",
                "TIER_10K",
                "TEST_TOKEN_SCHED"
        ));

        String phoneHash = com.example.wasaas.ledger.PhonePrivacyUtils.hashPhoneNumber("+919876543210");
        contactA = new Contact(tenantAId, "+919876543210", phoneHash, "Aarav Gupta");
        contactA = contactRepository.save(contactA);

        templateA = new WhatsAppTemplate(
                tenantAId,
                accountA.getId(),
                "meta_tpl_sched_01",
                "appointment_reminder_v1",
                "en_US",
                TemplateCategory.UTILITY,
                TemplateCategory.UTILITY,
                TemplateStatus.APPROVED,
                null,
                "Dear {{1}}, your appointment is at {{2}}.",
                null,
                null
        );
        templateA = templateRepository.save(templateA);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE scheduled_messages, faqs, unmatched_messages, automation_rules, whatsapp_templates, message_ledger_status_events, message_ledger, conversations, contacts, webhook_events, jobs, whatsapp_accounts, spring_session_attributes, spring_session, password_reset_tokens, login_attempts, tenant_users, users, tenants CASCADE");
    }

    @Test
    void testScheduleMessageInFutureWithTimezone() {
        TenantContext.set(tenantAId);

        Instant scheduledTime = Instant.now().plus(2, ChronoUnit.HOURS);
        List<TemplateComponent> components = List.of(
                TemplateComponent.body(List.of(
                        TemplateParameter.text("Aarav"),
                        TemplateParameter.text("5:00 PM")
                ))
        );

        ScheduledMessage message = schedulingService.scheduleMessage(new ScheduleMessageCommand(
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                components,
                scheduledTime,
                "Asia/Kolkata"
        ));

        assertThat(message.getId()).isNotNull();
        assertThat(message.getStatus()).isEqualTo(ScheduledMessageStatus.SCHEDULED);
        assertThat(message.getScheduledFor()).isEqualTo(scheduledTime);
        assertThat(message.getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void testEnqueueDueMessagesHandlerDispatchesSendJob() throws Exception {
        TenantContext.set(tenantAId);

        // Schedule a message that is already due (1 minute in the past)
        Instant pastTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        ScheduledMessage dueMessage = new ScheduledMessage(
                tenantAId,
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                null,
                pastTime,
                "Asia/Kolkata"
        );
        dueMessage = scheduledMessageRepository.save(dueMessage);

        // Execute due message processor
        enqueueHandler.handle(new Job(UUID.randomUUID(), tenantAId, "ENQUEUE_DUE_SCHEDULED_MESSAGES", "{}", com.example.wasaas.job.JobStatus.PENDING, "scan-01", 0, 5, Instant.now()));

        TenantContext.set(tenantAId);
        ScheduledMessage refreshed = scheduledMessageRepository.findById(dueMessage.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(ScheduledMessageStatus.ENQUEUED);

        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        Job sendJob = jobs.get(0);
        assertThat(sendJob.getJobType()).isEqualTo("SEND_WHATSAPP_MESSAGE");
        assertThat(sendJob.getIdempotencyKey()).contains("sched:" + dueMessage.getId());
    }

    @Test
    void testDoubleRunInSameMinuteDoesNotDoubleSend() throws Exception {
        TenantContext.set(tenantAId);

        Instant pastTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        ScheduledMessage dueMessage = new ScheduledMessage(
                tenantAId,
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                null,
                pastTime,
                "Asia/Kolkata"
        );
        scheduledMessageRepository.save(dueMessage);

        // 1st run
        enqueueHandler.handle(new Job(UUID.randomUUID(), tenantAId, "ENQUEUE_DUE_SCHEDULED_MESSAGES", "{}", com.example.wasaas.job.JobStatus.PENDING, "scan-01", 0, 5, Instant.now()));
        assertThat(jobRepository.findAll()).hasSize(1);

        // 2nd run in same minute
        enqueueHandler.handle(new Job(UUID.randomUUID(), tenantAId, "ENQUEUE_DUE_SCHEDULED_MESSAGES", "{}", com.example.wasaas.job.JobStatus.PENDING, "scan-02", 0, 5, Instant.now()));
        // Due message is already ENQUEUED, so exactly 1 send job exists
        assertThat(jobRepository.findAll()).hasSize(1);
    }

    @Test
    void testCancelWhileScheduledSucceeds() {
        TenantContext.set(tenantAId);

        Instant scheduledTime = Instant.now().plus(1, ChronoUnit.DAYS);
        ScheduledMessage message = new ScheduledMessage(
                tenantAId,
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                null,
                scheduledTime,
                "Asia/Kolkata"
        );
        message = scheduledMessageRepository.save(message);

        schedulingService.cancelScheduledMessage(message.getId());

        TenantContext.set(tenantAId);
        ScheduledMessage cancelled = scheduledMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ScheduledMessageStatus.CANCELLED);
    }

    @Test
    void testCancelWhileEnqueuedThrowsConflictException() {
        TenantContext.set(tenantAId);

        ScheduledMessage message = new ScheduledMessage(
                tenantAId,
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                null,
                Instant.now(),
                "Asia/Kolkata"
        );
        message.markEnqueued(null);
        ScheduledMessage enqueuedMessage = scheduledMessageRepository.save(message);

        assertThatThrownBy(() -> schedulingService.cancelScheduledMessage(enqueuedMessage.getId()))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException de = (DomainException) e;
                    assertThat(de.status()).isEqualTo(HttpStatus.CONFLICT);
                })
                .hasMessageContaining("Cannot cancel scheduled message in status: ENQUEUED");
    }

    @Test
    void testScheduleFreeTextRejectedAtCreation() {
        TenantContext.set(tenantAId);

        assertThatThrownBy(() -> schedulingService.scheduleMessage(new ScheduleMessageCommand(
                contactA.getId(),
                null, // Free-text without template
                accountA.getId(),
                null,
                Instant.now().plus(1, ChronoUnit.HOURS),
                "Asia/Kolkata"
        )))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("must use an approved template");
    }

    @Test
    void testMultiTenantScheduledMessageIsolation() {
        TenantContext.set(tenantAId);

        ScheduledMessage message = new ScheduledMessage(
                tenantAId,
                contactA.getId(),
                templateA.getId(),
                accountA.getId(),
                null,
                Instant.now().plus(1, ChronoUnit.HOURS),
                "Asia/Kolkata"
        );
        scheduledMessageRepository.save(message);

        // Register Tenant B
        tenantService.registerTenant(new RegistrationCommand(
                "Scheduling Store B",
                "scheduling-store-b",
                "Admin B",
                "admin.b@sched.com",
                "Password123!"
        ));
        UUID tenantBId = tenantRepository.findBySlug("scheduling-store-b").orElseThrow().getId();
        TenantContext.set(tenantBId);

        assertThat(scheduledMessageRepository.findAllByTenantId(tenantBId)).isEmpty();
        assertThat(scheduledMessageRepository.findByTenantIdAndId(tenantBId, message.getId())).isEmpty();
    }
}
