package com.tests;

import com.aventstack.extentreports.ExtentTest;
import com.keywords.KeywordContext;
import com.keywords.KeywordExecutor;
import com.model.TestCaseData;
import com.model.TestStep;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeywordDrivenApiTest extends BaseTest {

    private List<TestCaseData> cachedCases;

    @DataProvider(name = "azureTestCases")
    public Object[][] azureTestCases() throws Exception {
        if (cachedCases == null) {
            cachedCases = azureClient.loadTestCasesFromConfiguredSuite();
        }
        Object[][] data = new Object[cachedCases.size()][1];
        for (int i = 0; i < cachedCases.size(); i++) {
            data[i][0] = cachedCases.get(i);
        }
        return data;
    }

    @Test(dataProvider = "azureTestCases")
    public void runAzureTestCase(TestCaseData testCase) {
        ExtentTest testNode = extent.createTest(
                testCase.getTitle() + " (ID: " + testCase.getId() + ")"
        );

        // Create executor and register keywords (you'll add implementations here)
        KeywordExecutor executor = new KeywordExecutor();
        // Example (later):
        // executor.register("API_GET", new ApiGetKeywordHandler());
        // executor.register("DB_ASSERT", new DbAssertKeywordHandler());

        List<Map<String, String>> rows = testCase.getDataRows();
        if (rows.isEmpty()) {
            rows = Collections.singletonList(Collections.emptyMap());
        }

        for (Map<String, String> row : rows) {
            testNode.info("Executing data row: " + row);
            for (TestStep step : testCase.getSteps()) {
                String keyword = extractKeyword(step.getAction());
                ExtentTest stepNode = testNode.createNode(
                        "Step " + step.getId() + " [" + keyword + "]"
                ).info("Action: " + step.getAction() +
                        "<br/>Expected: " + step.getExpected());

                KeywordContext ctx = new KeywordContext(
                        testCase,
                        step,
                        row,
                        dbClient,
                        azureClient,
                        stepNode
                );

                executor.execute(keyword, ctx);
            }
        }
    }

    private String extractKeyword(String action) {
        if (action == null) {
            return "";
        }
        // Very simple HTML tag strip + first token logic
        String cleaned = action.replaceAll("<[^>]+>", " ").trim();
        String[] parts = cleaned.split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}
