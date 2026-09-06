package com.example.wasaas.broadcast;

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
public class DispatchBroadcastJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DispatchBroadcastJobHandler.class);
    public static final String JOB_TYPE = "DISPATCH_BROADCAST_CAMPAIGN";

    private final BroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    public DispatchBroadcastJobHandler(BroadcastService broadcastService, ObjectMapper objectMapper) {
        this.broadcastService = broadcastService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(Job job) throws Exception {
        JsonNode payloadNode = objectMapper.readTree(job.getPayload());
        if (!payloadNode.has("campaignId")) {
            throw new PermanentJobException("Malformed DISPATCH_BROADCAST_CAMPAIGN payload missing campaignId");
        }

        UUID campaignId = UUID.fromString(payloadNode.get("campaignId").asText());
        log.info("Processing background broadcast dispatch for campaign {}", campaignId);
        broadcastService.dispatchCampaignInternal(campaignId);
    }
}
