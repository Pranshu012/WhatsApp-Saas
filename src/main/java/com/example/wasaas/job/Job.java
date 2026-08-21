package com.example.wasaas.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "run_after", nullable = false)
    private Instant runAfter;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Job() {}

    public Job(UUID id, UUID tenantId, String jobType, String payload, JobStatus status,
               String idempotencyKey, int attempts, int maxAttempts, Instant runAfter) {
        this.id = id;
        this.tenantId = tenantId;
        this.jobType = jobType;
        this.payload = payload;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.runAfter = runAfter;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getJobType() { return jobType; }
    public String getPayload() { return payload; }
    public JobStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getRunAfter() { return runAfter; }
    public Instant getLockedAt() { return lockedAt; }
    public String getLockedBy() { return lockedBy; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters for job lifecycle
    public void setStatus(JobStatus status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setRunAfter(Instant runAfter) { this.runAfter = runAfter; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
