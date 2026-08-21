package com.example.wasaas.whatsapp.webhook;

import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobRepository;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class WebhookReceiverTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private MetaProperties metaProperties;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE webhook_events, jobs CASCADE");
    }

    private String computeHmacSignature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(metaProperties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void testVerificationHandshakeSuccess() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", metaProperties.getWebhookVerifyToken())
                        .param("hub.challenge", "CHALLENGE_ACCEPTED_12345"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("CHALLENGE_ACCEPTED_12345"));
    }

    @Test
    void testVerificationHandshakeWrongTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "WRONG_TOKEN_VALUE")
                        .param("hub.challenge", "CHALLENGE_ACCEPTED_12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testVerificationHandshakeWrongModeReturns403() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "unsubscribe")
                        .param("hub.verify_token", metaProperties.getWebhookVerifyToken())
                        .param("hub.challenge", "CHALLENGE_ACCEPTED_12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReceiveValidWebhookSignature200AndPersisted() throws Exception {
        String payloadJson = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "1122334455",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "display_phone_number": "15550100",
                          "phone_number_id": "9988776655"
                        },
                        "messages": [{
                          "from": "15551234567",
                          "id": "wamid.HBgLMSG001",
                          "timestamp": "1700000000",
                          "text": { "body": "Hello support!" },
                          "type": "text"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """;

        byte[] rawBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacSignature(rawBytes);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBytes))
                .andExpect(status().isOk());

        // Verify webhook_events table persistence
        List<WebhookEvent> events = webhookEventRepository.findAll();
        assertThat(events).hasSize(1);
        WebhookEvent event = events.get(0);
        assertThat(event.getEventId()).isEqualTo("wamid.HBgLMSG001");
        assertThat(event.getWabaId()).isEqualTo("1122334455");
        assertThat(event.getPhoneNumberId()).isEqualTo("9988776655");
        assertThat(event.isSignatureValid()).isTrue();
        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.PENDING);
        assertThat(event.getRawPayload()).contains("Hello support!");

        // Verify background job queued
        List<Job> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        Job job = jobs.get(0);
        assertThat(job.getJobType()).isEqualTo("PROCESS_WEBHOOK_EVENT");
        assertThat(job.getPayload()).contains(event.getId().toString());
        assertThat(job.getIdempotencyKey()).isEqualTo("wh:wamid.HBgLMSG001");
    }

    @Test
    void testTamperedBodyReturns403() throws Exception {
        byte[] originalBytes = "{\"test\": \"payload\"}".getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacSignature(originalBytes);

        byte[] tamperedBytes = "{\"test\": \"tampered_payload\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tamperedBytes))
                .andExpect(status().isForbidden());

        assertThat(webhookEventRepository.findAll()).isEmpty();
        assertThat(jobRepository.findAll()).isEmpty();
    }

    @Test
    void testMissingSignatureReturns403() throws Exception {
        byte[] bytes = "{\"test\": \"payload\"}".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bytes))
                .andExpect(status().isForbidden());

        assertThat(webhookEventRepository.findAll()).isEmpty();
        assertThat(jobRepository.findAll()).isEmpty();
    }

    @Test
    void testDuplicateEventProducesOneEffect() throws Exception {
        String payloadJson = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "1122334455",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {
                          "phone_number_id": "9988776655"
                        },
                        "messages": [{
                          "id": "wamid.HBgLDUP999"
                        }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """;

        byte[] rawBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacSignature(rawBytes);

        // First delivery -> 200 OK
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBytes))
                .andExpect(status().isOk());

        // Second duplicate delivery -> 200 OK (Meta does not retry)
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Hub-Signature-256", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawBytes))
                .andExpect(status().isOk());

        // Exactly one event and one job persisted
        assertThat(webhookEventRepository.findAll()).hasSize(1);
        assertThat(jobRepository.findAll()).hasSize(1);
    }

    @Test
    void testFastAckLatency() throws Exception {
        String payloadJson = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "1122334455",
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": { "phone_number_id": "9988776655" },
                        "statuses": [{ "id": "wamid.BENCH", "status": "sent" }]
                      },
                      "field": "messages"
                    }]
                  }]
                }
                """;

        byte[] rawBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        String signature = computeHmacSignature(rawBytes);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/webhooks/whatsapp")
                            .header("X-Hub-Signature-256", signature)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rawBytes))
                    .andExpect(status().isOk());
        }
        long duration = System.currentTimeMillis() - start;
        long avgMs = duration / 20;

        // Ingest ACK latency should be well under 100ms per request (target < 2000ms)
        assertThat(avgMs).isLessThan(500);
    }
}
