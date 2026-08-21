package com.example.wasaas.ledger;

import com.example.wasaas.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_ledger")
public class MessageLedger extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "whatsapp_account_id")
    private UUID whatsappAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, updatable = false)
    private MessageDirection direction;

    @Column(name = "wamid")
    private String wamid;

    @Column(name = "recipient_phone_hash", nullable = false, updatable = false)
    private String recipientPhoneHash;

    @Column(name = "recipient_phone_last4", nullable = false, updatable = false, length = 4)
    private String recipientPhoneLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_category", nullable = false, updatable = false)
    private BillingCategory billingCategory;

    @Column(name = "template_name")
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_window")
    private ConversationWindow conversationWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageLedgerStatus status;

    @Column(name = "status_at", nullable = false)
    private Instant statusAt;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MessageLedger() {}

    public MessageLedger(UUID whatsappAccountId,
                         MessageDirection direction,
                         String recipientPhoneHash,
                         String recipientPhoneLast4,
                         BillingCategory billingCategory,
                         String templateName,
                         ConversationWindow conversationWindow,
                         MessageLedgerStatus status,
                         String idempotencyKey,
                         UUID jobId) {
        this.whatsappAccountId = whatsappAccountId;
        this.direction = direction;
        this.recipientPhoneHash = recipientPhoneHash;
        this.recipientPhoneLast4 = recipientPhoneLast4;
        this.billingCategory = billingCategory;
        this.templateName = templateName;
        this.conversationWindow = conversationWindow;
        this.status = status;
        this.statusAt = Instant.now();
        this.idempotencyKey = idempotencyKey;
        this.jobId = jobId;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.statusAt == null) {
            this.statusAt = this.createdAt;
        }
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getWhatsappAccountId() { return whatsappAccountId; }
    public MessageDirection getDirection() { return direction; }
    public String getWamid() { return wamid; }
    public String getRecipientPhoneHash() { return recipientPhoneHash; }
    public String getRecipientPhoneLast4() { return recipientPhoneLast4; }
    public BillingCategory getBillingCategory() { return billingCategory; }
    public String getTemplateName() { return templateName; }
    public ConversationWindow getConversationWindow() { return conversationWindow; }
    public MessageLedgerStatus getStatus() { return status; }
    public Instant getStatusAt() { return statusAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getJobId() { return jobId; }
    public Integer getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }

    // State update setters
    public void setWamid(String wamid) { this.wamid = wamid; }
    public void setStatus(MessageLedgerStatus status) {
        this.status = status;
        this.statusAt = Instant.now();
    }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
}
