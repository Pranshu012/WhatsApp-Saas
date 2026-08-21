package com.example.wasaas.whatsapp.meta;

import com.example.wasaas.common.exception.DomainException;
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

@Component
public class MetaGraphClient {

    private static final Logger log = LoggerFactory.getLogger(MetaGraphClient.class);

    private RestClient restClient;
    private final MetaProperties metaProperties;
    private final ObjectMapper objectMapper;

    public MetaGraphClient(MetaProperties metaProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
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

    public String exchangeCodeForToken(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code cannot be empty");
        }

        String uri = metaProperties.getApiBaseUrl() + "/oauth/access_token"
                + "?client_id=" + metaProperties.getAppId()
                + "&client_secret=" + metaProperties.getAppSecret()
                + "&code=" + code;

        MetaTokenResponse response = restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MetaTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta code exchange returned an empty access token");
        }

        return response.accessToken();
    }

    public MetaWabaDetails getWabaDetails(String wabaId, String accessToken) {
        String uri = metaProperties.getApiBaseUrl() + "/" + wabaId
                + "?fields=id,name,timezone_id,message_template_namespace";

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MetaWabaDetails.class);
    }

    public MetaPhoneNumberDetails getPhoneNumberDetails(String phoneNumberId, String accessToken) {
        String uri = metaProperties.getApiBaseUrl() + "/" + phoneNumberId
                + "?fields=id,display_phone_number,verified_name,quality_rating,messaging_limit_tier";

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MetaPhoneNumberDetails.class);
    }

    public void subscribeAppToWaba(String wabaId, String accessToken) {
        String uri = metaProperties.getApiBaseUrl() + "/" + wabaId + "/subscribed_apps";

        MetaSubscribedAppsResponse response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MetaSubscribedAppsResponse.class);

        if (response == null || !response.success()) {
            throw new DomainException(HttpStatus.BAD_GATEWAY, "Failed to subscribe app to WABA webhooks");
        }
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

                log.warn("Meta Graph API returned error code [{}]: {}", code, message);

                if (code == 200) {
                    throw new DomainException(
                            HttpStatus.FAILED_DEPENDENCY,
                            "Meta App lacks Advanced Access for WhatsApp Business Management. Please complete App Review in the Meta App Dashboard."
                    );
                }
                if (code == 190) {
                    throw new DomainException(HttpStatus.UNAUTHORIZED, "WhatsApp access token is invalid or expired");
                }

                throw new DomainException(HttpStatus.BAD_GATEWAY, "Meta Graph API error [" + code + "]: " + message);
            }
        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            // Not a standard MetaErrorResponse JSON
        }

        throw new DomainException(
                HttpStatus.valueOf(response.getStatusCode().value()),
                "Meta Graph API error: " + bodyString
        );
    }

    public RestClient getRestClient() {
        return restClient;
    }
}
