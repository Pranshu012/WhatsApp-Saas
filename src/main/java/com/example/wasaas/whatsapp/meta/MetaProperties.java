package com.example.wasaas.whatsapp.meta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.meta")
public class MetaProperties {

    private String graphBaseUrl = "https://graph.facebook.com";
    private String graphVersion = "v20.0";
    private String appId = "";
    private String appSecret = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    public String getGraphBaseUrl() { return graphBaseUrl; }
    public void setGraphBaseUrl(String graphBaseUrl) { this.graphBaseUrl = graphBaseUrl; }

    public String getGraphVersion() { return graphVersion; }
    public void setGraphVersion(String graphVersion) { this.graphVersion = graphVersion; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public String getApiBaseUrl() {
        return graphBaseUrl.replaceAll("/+$", "") + "/" + graphVersion;
    }
}
