package com.keywords;

import com.aventstack.extentreports.ExtentTest;
import com.azure.AzureDevOpsClient;
import com.OracleDbClient;
import com.model.TestCaseData;
import com.model.TestStep;

import java.util.Map;

public class KeywordContext {

    private final TestCaseData testCase;
    private final TestStep step;
    private final Map<String, String> dataRow;
    private final OracleDbClient dbClient;
    private final AzureDevOpsClient azureClient;
    private final ExtentTest extentTest;

    public KeywordContext(TestCaseData testCase,
                          TestStep step,
                          Map<String, String> dataRow,
                          OracleDbClient dbClient,
                          AzureDevOpsClient azureClient,
                          ExtentTest extentTest) {
        this.testCase = testCase;
        this.step = step;
        this.dataRow = dataRow;
        this.dbClient = dbClient;
        this.azureClient = azureClient;
        this.extentTest = extentTest;
    }

    public TestCaseData getTestCase() {
        return testCase;
    }

    public TestStep getStep() {
        return step;
    }

    public Map<String, String> getDataRow() {
        return dataRow;
    }

    public OracleDbClient getDbClient() {
        return dbClient;
    }

    public AzureDevOpsClient getAzureClient() {
        return azureClient;
    }

    public ExtentTest getExtentTest() {
        return extentTest;
    }
}
