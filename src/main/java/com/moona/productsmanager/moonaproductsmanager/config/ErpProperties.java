package com.moona.productsmanager.moonaproductsmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "erp")
public class ErpProperties {
    private String baseUrl;
    private String loginPath;
    private String reportPath;
    private String username;
    private String password;
    private String defaultChannelId;
    private String defaultWarehouseId;
    private String defaultCategoryId;
    private boolean defaultPublished = true;
    private boolean defaultTrackInventory = true;
    private String defaultWeighted = "false";
    private String defaultMinAmount = "10";
    private int connectTimeoutMs = 15000;
    private int readTimeoutMs = 30000;
    private int maxInMemoryBytes = 2 * 1024 * 1024;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDefaultChannelId() {
        return defaultChannelId;
    }

    public void setDefaultChannelId(String defaultChannelId) {
        this.defaultChannelId = defaultChannelId;
    }

    public String getDefaultWarehouseId() {
        return defaultWarehouseId;
    }

    public void setDefaultWarehouseId(String defaultWarehouseId) {
        this.defaultWarehouseId = defaultWarehouseId;
    }

    public String getDefaultCategoryId() {
        return defaultCategoryId;
    }

    public void setDefaultCategoryId(String defaultCategoryId) {
        this.defaultCategoryId = defaultCategoryId;
    }

    public boolean isDefaultPublished() {
        return defaultPublished;
    }

    public void setDefaultPublished(boolean defaultPublished) {
        this.defaultPublished = defaultPublished;
    }

    public boolean isDefaultTrackInventory() {
        return defaultTrackInventory;
    }

    public void setDefaultTrackInventory(boolean defaultTrackInventory) {
        this.defaultTrackInventory = defaultTrackInventory;
    }

    public String getDefaultWeighted() {
        return defaultWeighted;
    }

    public void setDefaultWeighted(String defaultWeighted) {
        this.defaultWeighted = defaultWeighted;
    }

    public String getDefaultMinAmount() {
        return defaultMinAmount;
    }

    public void setDefaultMinAmount(String defaultMinAmount) {
        this.defaultMinAmount = defaultMinAmount;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxInMemoryBytes() {
        return maxInMemoryBytes;
    }

    public void setMaxInMemoryBytes(int maxInMemoryBytes) {
        this.maxInMemoryBytes = maxInMemoryBytes;
    }
}
