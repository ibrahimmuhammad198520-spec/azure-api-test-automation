package com.tests;

import com.aventstack.extentreports.ExtentReports;
import com.azure.AzureDevOpsClient;
import com.config.FrameworkConfig;
import com.OracleDbClient;
import com.reporting.ExtentManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public abstract class BaseTest {

    protected static FrameworkConfig config;
    protected static AzureDevOpsClient azureClient;
    protected static OracleDbClient dbClient;
    protected static ExtentReports extent;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        config = new FrameworkConfig();
        extent = ExtentManager.createInstance(config.getReportPath());
        azureClient = new AzureDevOpsClient(config);
        dbClient = new OracleDbClient(config);
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        if (dbClient != null) {
            dbClient.close();
        }
        if (extent != null) {
            extent.flush();
        }
    }
}
