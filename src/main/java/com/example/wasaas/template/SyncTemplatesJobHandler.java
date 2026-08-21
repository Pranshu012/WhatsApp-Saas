package com.example.wasaas.template;

import com.example.wasaas.job.Job;
import com.example.wasaas.job.JobHandler;
import com.example.wasaas.job.PermanentJobException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SyncTemplatesJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncTemplatesJobHandler.class);
    public static final String JOB_TYPE = "SYNC_TEMPLATES";

    private final TemplateSyncService syncService;
    private final ObjectMapper objectMapper;

    public SyncTemplatesJobHandler(TemplateSyncService syncService, ObjectMapper objectMapper) {
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        JsonNode payloadNode = objectMapper.readTree(job.getPayload());
        if (!payloadNode.has("whatsappAccountId")) {
            throw new PermanentJobException("Malformed SYNC_TEMPLATES payload missing whatsappAccountId");
        }

        UUID whatsappAccountId = UUID.fromString(payloadNode.get("whatsappAccountId").asText());
        UUID tenantId = job.getTenantId();
        if (tenantId == null && payloadNode.has("tenantId")) {
            tenantId = UUID.fromString(payloadNode.get("tenantId").asText());
        }

        if (tenantId == null) {
            throw new PermanentJobException("SYNC_TEMPLATES job missing tenantId");
        }

        int synced = syncService.syncTemplates(tenantId, whatsappAccountId);
        log.info("Successfully synced {} templates for tenant [{}]", synced, tenantId);
    }
}
