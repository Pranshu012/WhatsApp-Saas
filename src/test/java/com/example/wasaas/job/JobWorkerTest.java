package com.example.wasaas.job;

import com.example.wasaas.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jobs.poll-interval-ms=1000000", // Disable scheduled polling for manual control
    "app.jobs.batch-size=5",
    "app.jobs.lock-timeout-secs=5"
})
@ActiveProfiles({"local", "worker"})
public class JobWorkerTest {

    @Autowired private JobService jobService;
    @Autowired private JobRepository jobRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JobWorker jobWorker;

    @Autowired private com.example.wasaas.tenant.TenantService tenantService;
    @Autowired private com.example.wasaas.tenant.TenantRepository tenantRepository;
    
    private UUID testTenantId;

    @BeforeEach
    void setup() {
        cleanup();
        tenantService.registerTenant(new com.example.wasaas.tenant.RegistrationCommand(
                "Job Test Biz",
                "job-test-biz",
                "Job Admin",
                "admin.job@example.com",
                "Password123!"
        ));
        testTenantId = tenantRepository.findBySlug("job-test-biz").orElseThrow().getId();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        jdbcTemplate.execute("TRUNCATE TABLE jobs, tenant_users, users, tenants CASCADE");
        TestJobHandler.reset();
    }

    @Test
    void testConcurrentWorkersNeverClaimSameJob() throws InterruptedException {
        // Enqueue 10 jobs
        for (int i = 0; i < 10; i++) {
            jobService.enqueue(testTenantId, "TEST_JOB", "{\"index\":" + i + "}", null);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger worker1Claims = new AtomicInteger(0);
        AtomicInteger worker2Claims = new AtomicInteger(0);

        Runnable worker1 = () -> {
            try {
                startLatch.await();
                List<Job> claimed = jobService.claimJobs("worker1", 5, Instant.now().minusSeconds(300));
                worker1Claims.set(claimed.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable worker2 = () -> {
            try {
                startLatch.await();
                List<Job> claimed = jobService.claimJobs("worker2", 5, Instant.now().minusSeconds(300));
                worker2Claims.set(claimed.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(worker1);
        executor.submit(worker2);

        // Unleash both workers simultaneously
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        // They should have claimed all 10 jobs safely (5 each if strictly fair, but sum must be 10)
        assertThat(worker1Claims.get() + worker2Claims.get()).isEqualTo(10);

        List<Job> runningJobs = jobRepository.findAll();
        assertThat(runningJobs).hasSize(10);
        assertThat(runningJobs).allMatch(j -> j.getStatus() == JobStatus.RUNNING);
        
        long worker1Count = runningJobs.stream().filter(j -> "worker1".equals(j.getLockedBy())).count();
        long worker2Count = runningJobs.stream().filter(j -> "worker2".equals(j.getLockedBy())).count();
        
        assertThat(worker1Count).isEqualTo(worker1Claims.get());
        assertThat(worker2Count).isEqualTo(worker2Claims.get());
    }

    @Test
    void testStaleLockRecovery() {
        jobService.enqueue(testTenantId, "TEST_JOB", "{}", null);
        
        // Manually lock it to simulate a dead worker
        Job job = jobRepository.findAll().get(0);
        job.setStatus(JobStatus.RUNNING);
        job.setLockedBy("dead_worker");
        job.setLockedAt(Instant.now().minusSeconds(10)); // Older than 5s lock timeout
        jobRepository.save(job);

        // A new worker should be able to claim it
        Instant staleThreshold = Instant.now().minusSeconds(5);
        List<Job> claimed = jobService.claimJobs("live_worker", 5, staleThreshold);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getLockedBy()).isEqualTo("live_worker");
    }

    @Test
    void testTransientFailureBackoffAndMaxAttempts() {
        jobService.enqueue(testTenantId, "FAIL_TRANSIENT", "{}", null);

        // Attempt 1
        jobWorker.poll();
        Job job = jobRepository.findAll().get(0);
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getRunAfter()).isAfter(Instant.now());

        // Force runAfter to now so we can poll again
        job.setRunAfter(Instant.now().minusSeconds(1));
        jobRepository.save(job);

        // Attempt 2
        jobWorker.poll();
        job = jobRepository.findAll().get(0);
        assertThat(job.getAttempts()).isEqualTo(2);

        job.setRunAfter(Instant.now().minusSeconds(1));
        jobRepository.save(job);

        // Attempt 3 (hits max attempts = 3)
        jobWorker.poll();
        job = jobRepository.findAll().get(0);
        assertThat(job.getAttempts()).isEqualTo(3);
        assertThat(job.getStatus()).isEqualTo(JobStatus.DEAD);
        assertThat(job.getLastError()).isEqualTo("Transient error");
    }

    @Test
    void testPermanentFailureGoesStraightToDead() {
        jobService.enqueue(testTenantId, "FAIL_PERMANENT", "{}", null);

        jobWorker.poll();

        Job job = jobRepository.findAll().get(0);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(JobStatus.DEAD);
        assertThat(job.getLastError()).isEqualTo("Permanent error");
    }

    @Test
    void testDuplicateIdempotencyKeyIgnored() {
        jobService.enqueue(testTenantId, "TEST_JOB", "{}", "key-123");
        jobService.enqueue(testTenantId, "TEST_JOB", "{}", "key-123");
        
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
    }
}

@Configuration
class TestJobConfig {
    @Bean
    public JobHandler testJobHandler() {
        return new TestJobHandler();
    }
}

class TestJobHandler implements JobHandler {
    private static int successCount = 0;

    @Override
    public String jobType() {
        return "TEST_JOB";
    }

    @Override
    public void handle(Job job) throws Exception {
        successCount++;
    }

    public static void reset() {
        successCount = 0;
    }
}

@Configuration
class FailTransientJobConfig {
    @Bean
    public JobHandler failTransientJobHandler() {
        return new JobHandler() {
            @Override
            public String jobType() { return "FAIL_TRANSIENT"; }

            @Override
            public void handle(Job job) throws Exception {
                throw new RuntimeException("Transient error");
            }
        };
    }
}

@Configuration
class FailPermanentJobConfig {
    @Bean
    public JobHandler failPermanentJobHandler() {
        return new JobHandler() {
            @Override
            public String jobType() { return "FAIL_PERMANENT"; }

            @Override
            public void handle(Job job) throws Exception {
                throw new PermanentJobException("Permanent error");
            }
        };
    }
}
