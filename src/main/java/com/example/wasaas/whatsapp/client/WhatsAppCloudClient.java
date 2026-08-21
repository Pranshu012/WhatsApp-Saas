package com.example.wasaas.whatsapp.client;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.job.PermanentJobException;
import com.example.wasaas.whatsapp.meta.MetaErrorResponse;
import com.example.wasaas.whatsapp.meta.MetaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WhatsAppCloudClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudClient.class);

    private RestClient restClient;
    private final MetaProperties metaProperties;
    private final ObjectMapper objectMapper;

    public WhatsAppCloudClient(MetaProperties metaProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.metaProperties = metaProperties;
        this.objectMapper = objectMapper;
        this.restClient = configureBuilder(restClientBuilder).build();
    }

    public RestClient.Builder createClientBuilder() {
        return configureBuilder(RestClient.builder());
    }

    public RestClient.Builder configureBuilder(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(metaProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(metaProperties.getReadTimeoutMs());

        return builder
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> handleMetaErrorResponse(resp));
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public SendResult sendText(String phoneNumberId, String decryptedToken, String toE164, String text) {
        String uri = metaProperties.getApiBaseUrl() + "/" + phoneNumberId + "/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toE164);
        payload.put("type", "text");
        payload.put("text", Map.of("preview_url", false, "body", text));

        MetaSendMessageResponse response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + decryptedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MetaSendMessageResponse.class);

        return extractSendResult(response);
    }

    public SendResult sendTemplate(String phoneNumberId, String decryptedToken, String toE164,
                                   String templateName, String languageCode, List<TemplateComponent> components) {
        String uri = metaProperties.getApiBaseUrl() + "/" + phoneNumberId + "/messages";

        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("name", templateName);
        templateMap.put("language", Map.of("code", languageCode != null ? languageCode : "en_US"));
        if (components != null && !components.isEmpty()) {
            templateMap.put("components", components);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toE164);
        payload.put("type", "template");
        payload.put("template", templateMap);

        MetaSendMessageResponse response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + decryptedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MetaSendMessageResponse.class);

        return extractSendResult(response);
    }

    public SendResult sendInteractiveButtons(String phoneNumberId, String decryptedToken, String toE164,
                                            String bodyText, List<ReplyButton> buttons) {
        String uri = metaProperties.getApiBaseUrl() + "/" + phoneNumberId + "/messages";

        List<Map<String, Object>> buttonList = new java.util.ArrayList<>();
        if (buttons != null) {
            for (ReplyButton btn : buttons) {
                buttonList.add(Map.of(
                        "type", "reply",
                        "reply", Map.of("id", btn.id(), "title", btn.title())
                ));
            }
        }

        Map<String, Object> interactiveMap = Map.of(
                "type", "button",
                "body", Map.of("text", bodyText != null ? bodyText : ""),
                "action", Map.of("buttons", buttonList)
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toE164,
                "type", "interactive",
                "interactive", interactiveMap
        );

        MetaSendMessageResponse response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + decryptedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MetaSendMessageResponse.class);

        return extractSendResult(response);
    }

    public SendResult sendInteractiveList(String phoneNumberId, String decryptedToken, String toE164,
                                          String bodyText, String buttonText, List<ListSection> sections) {
        String uri = metaProperties.getApiBaseUrl() + "/" + phoneNumberId + "/messages";

        List<Map<String, Object>> sectionList = new java.util.ArrayList<>();
        if (sections != null) {
            for (ListSection sec : sections) {
                List<Map<String, Object>> rowList = new java.util.ArrayList<>();
                if (sec.rows() != null) {
                    for (ListRow row : sec.rows()) {
                        Map<String, Object> rowMap = new HashMap<>();
                        rowMap.put("id", row.id());
                        rowMap.put("title", row.title());
                        if (row.description() != null && !row.description().isBlank()) {
                            rowMap.put("description", row.description());
                        }
                        rowList.add(rowMap);
                    }
                }
                sectionList.add(Map.of(
                        "title", sec.title() != null ? sec.title() : "",
                        "rows", rowList
                ));
            }
        }

        Map<String, Object> interactiveMap = Map.of(
                "type", "list",
                "body", Map.of("text", bodyText != null ? bodyText : ""),
                "action", Map.of(
                        "button", buttonText != null ? buttonText : "Select",
                        "sections", sectionList
                )
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toE164,
                "type", "interactive",
                "interactive", interactiveMap
        );

        MetaSendMessageResponse response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + decryptedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MetaSendMessageResponse.class);

        return extractSendResult(response);
    }

    private SendResult extractSendResult(MetaSendMessageResponse response) {
        if (response == null || response.messages() == null || response.messages().isEmpty()) {
            throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta Cloud API returned empty messages array in send response");
        }
        String wamid = response.messages().get(0).id();
        if (wamid == null || wamid.isBlank()) {
            throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta Cloud API returned blank message id");
        }
        return new SendResult(wamid);
    }

    private void handleMetaErrorResponse(ClientHttpResponse response) throws IOException {
        byte[] bodyBytes = response.getBody().readAllBytes();
        String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);

        try {
            MetaErrorResponse errorResponse = objectMapper.readValue(bodyString, MetaErrorResponse.class);
            if (errorResponse != null && errorResponse.error() != null) {
                MetaErrorResponse.MetaError err = errorResponse.error();
                int code = err.code();
                String message = err.message();

                log.warn("Meta Cloud API send returned error code [{}]: {}", code, message);

                // Permanent Errors
                if (code == 200) {
                    throw new PermanentJobException("Meta App lacks Advanced Access for WhatsApp Business Messaging");
                }
                if (code == 190) {
                    throw new MetaTokenRevokedException("WhatsApp access token is invalid or expired (Error 190)");
                }
                if (code == 131026) {
                    throw new PermanentJobException("Recipient is not a valid WhatsApp user (Error 131026)");
                }
                if (code == 131047) {
                    throw new PermanentJobException("Cannot send free-form message outside 24-hour service window without a template (Error 131047)");
                }
                if (code == 132000 || code == 132001) {
                    throw new PermanentJobException("Template parameter mismatch or template not found (Error " + code + ")");
                }

                // Transient Errors (Rate limits & Server errors)
                if (code == 130429) {
                    throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "Meta rate limit exceeded (Error 130429)");
                }
                if (code == 131048) {
                    throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "Spam rate limit hit (Error 131048)");
                }
                if (code == 1 || code == 2) {
                    throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta internal server error (Error " + code + ")");
                }

                if (response.getStatusCode().is4xxClientError()) {
                    throw new PermanentJobException("Meta client error [" + code + "]: " + message);
                }

                throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta Graph API error [" + code + "]: " + message);
            }
        } catch (PermanentJobException | DomainException e) {
            throw e;
        } catch (Exception e) {
            // Not a standard MetaErrorResponse JSON
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new PermanentJobException("Meta client error HTTP " + response.getStatusCode().value() + ": " + bodyString);
        }

        throw new DomainException(
                HttpStatus.valueOf(response.getStatusCode().value()),
                "Meta Cloud API error: " + bodyString
        );
    }
}
