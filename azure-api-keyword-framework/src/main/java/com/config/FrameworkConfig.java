package com.config;

import java.io.InputStream;
import java.util.Properties;

public class FrameworkConfig {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties props = new Properties();

    public FrameworkConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public String getAzureProjectUrl() {
        return get("azure.projectUrl", null);
    }

    public int getAzurePlanId() {
        return Integer.parseInt(get("azure.planId", "0"));
    }

    public int getAzureSuiteId() {
        return Integer.parseInt(get("azure.suiteId", "0"));
    }

    public String getAzureApiVersion() {
        return get("azure.apiVersion", "6.0");
    }

    public String getAzurePat() {
        return get("azure.pat", null);
    }

    public String getDbUrl() {
        return get("db.url", null);
    }

    public String getDbUser() {
        return get("db.user", null);
    }

    public String getDbPassword() {
        return get("db.password", null);
    }

    public String getReportPath() {
        return get("report.path", "reports/AutomationReport.html");
    }
}
