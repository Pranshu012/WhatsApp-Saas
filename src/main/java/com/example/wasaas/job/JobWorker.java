package com.example.wasaas.job;

import com.example.wasaas.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("worker")
@EnableScheduling
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();
    private final String workerId = UUID.randomUUID().toString();

    @Value("${app.jobs.batch-size:10}")
    private int batchSize;

    @Value("${app.jobs.lock-timeout-secs:300}")
    private int lockTimeoutSecs;

    public JobWorker(JobService jobService, JobRepository jobRepository, List<JobHandler> jobHandlers) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        for (JobHandler handler : jobHandlers) {
            this.handlers.put(handler.jobType(), handler);
        }
        log.info("JobWorker initialized with ID {} and {} handlers", workerId, handlers.size());
    }

    @Scheduled(fixedDelayString = "${app.jobs.poll-interval-ms:1000}")
    public void poll() {
        Instant staleThreshold = Instant.now().minus(lockTimeoutSecs, ChronoUnit.SECONDS);
        List<Job> claimedJobs = jobService.claimJobs(workerId, batchSize, staleThreshold);

        for (Job job : claimedJobs) {
            JobHandler handler = handlers.get(job.getJobType());
            jobService.processJob(job, handler);
        }
    }
}
