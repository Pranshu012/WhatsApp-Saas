package com.example.wasaas.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JdbcTemplate jdbcTemplate;
    private final JobRepository jobRepository;

    public JobService(JdbcTemplate jdbcTemplate, JobRepository jobRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void enqueue(UUID tenantId, String jobType, String payload, String idempotencyKey) {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();

        String sql;
        if (idempotencyKey != null) {
            sql = """
                INSERT INTO jobs (id, tenant_id, job_type, payload, status, idempotency_key, attempts, max_attempts, run_after, created_at, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idempotency_key) WHERE idempotency_key IS NOT NULL DO NOTHING
            """;
        } else {
            sql = """
                INSERT INTO jobs (id, tenant_id, job_type, payload, status, idempotency_key, attempts, max_attempts, run_after, created_at, updated_at)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
            """;
        }

        int rows = jdbcTemplate.update(sql,
                jobId,
                tenantId,
                jobType,
                payload,
                JobStatus.PENDING.name(),
                idempotencyKey,
                0,
                3,
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now)
        );

        if (rows > 0) {
            log.debug("Enqueued job {} of type {}", jobId, jobType);
        } else {
            log.debug("Job skipped due to idempotency key {}", idempotencyKey);
        }
    }

    @Transactional
    public List<Job> claimJobs(String workerId, int limit, Instant staleThreshold) {
        Instant now = Instant.now();

        String claimSql = """
            SELECT id FROM jobs
            WHERE (status = 'PENDING' AND run_after <= ?)
               OR (status = 'RUNNING' AND locked_at <= ?)
            ORDER BY run_after ASC
            FOR UPDATE SKIP LOCKED
            LIMIT ?
        """;

        List<UUID> jobIds = jdbcTemplate.queryForList(claimSql, UUID.class, java.sql.Timestamp.from(now), java.sql.Timestamp.from(staleThreshold), limit);

        if (jobIds.isEmpty()) {
            return List.of();
        }

        String updateSql = """
            UPDATE jobs
            SET status = 'RUNNING',
                locked_at = ?,
                locked_by = ?,
                attempts = attempts + 1,
                updated_at = ?
            WHERE id IN (%s)
        """.formatted(String.join(",", jobIds.stream().map(id -> "?").toList()));

        Object[] params = new Object[jobIds.size() + 3];
        params[0] = java.sql.Timestamp.from(now);
        params[1] = workerId;
        params[2] = java.sql.Timestamp.from(now);
        for (int i = 0; i < jobIds.size(); i++) {
            params[i + 3] = jobIds.get(i);
        }

        jdbcTemplate.update(updateSql, params);

        return jobRepository.findAllById(jobIds);
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processJob(Job job, JobHandler handler) {
        if (handler == null) {
            log.error("No handler found for job type {}, marking DEAD", job.getJobType());
            job.setStatus(JobStatus.DEAD);
            job.setLastError("No handler registered for type " + job.getJobType());
            jobRepository.save(job);
            return;
        }

        UUID previousTenant = com.example.wasaas.tenant.context.TenantContext.get();
        try {
            if (job.getTenantId() != null) {
                com.example.wasaas.tenant.context.TenantContext.set(job.getTenantId());
            }

            handler.handle(job);

            job.setStatus(JobStatus.SUCCEEDED);
            jobRepository.save(job);

        } catch (PermanentJobException e) {
            log.warn("Permanent failure for job {}", job.getId(), e);
            job.setStatus(JobStatus.DEAD);
            job.setLastError(e.getMessage());
            jobRepository.save(job);
        } catch (Exception e) {
            log.warn("Transient failure for job {}", job.getId(), e);
            job.setLastError(e.getMessage());
            if (job.getAttempts() >= job.getMaxAttempts()) {
                job.setStatus(JobStatus.DEAD);
            } else {
                job.setStatus(JobStatus.PENDING);
                long delaySeconds = (long) Math.pow(2, job.getAttempts());
                delaySeconds = Math.min(delaySeconds, 86400); // Max 1 day
                job.setRunAfter(Instant.now().plusSeconds(delaySeconds));
                job.setLockedAt(null);
                job.setLockedBy(null);
            }
            jobRepository.save(job);
        } finally {
            if (previousTenant != null) {
                com.example.wasaas.tenant.context.TenantContext.set(previousTenant);
            } else {
                com.example.wasaas.tenant.context.TenantContext.clear();
            }
        }
    }
}
