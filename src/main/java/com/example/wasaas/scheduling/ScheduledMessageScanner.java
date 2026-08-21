package com.example.wasaas.scheduling;

import com.example.wasaas.job.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Profile("worker")
public class ScheduledMessageScanner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageScanner.class);

    private final JobService jobService;

    public ScheduledMessageScanner(JobService jobService) {
        this.jobService = jobService;
    }

    @Scheduled(cron = "${app.scheduling.cron:0 * * * * *}")
    public void scanDueMessages() {
        // Deterministic scan key per minute: due-scan:{minute_epoch}
        long currentMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
        String idempotencyKey = "due-scan:" + currentMinute;

        try {
            jobService.enqueue(
                    null, // System-level job
                    EnqueueDueScheduledMessagesHandler.JOB_TYPE,
                    "{}",
                    idempotencyKey
            );
            log.debug("Enqueued due message scan job with key [{}]", idempotencyKey);
        } catch (Exception e) {
            log.debug("Due message scan job already enqueued for minute [{}]", currentMinute);
        }
    }
}
