package com.azure;

import com.config.FrameworkConfig;
import com.model.TestCaseData;
import com.model.TestStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Client to talk to Azure DevOps Server 2020 using REST
 * and map test plan/suite test cases to our TestCaseData model.
 */
public class AzureDevOpsClient {

    private final FrameworkConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String authHeader;

    public AzureDevOpsClient(FrameworkConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();

        String pat = Optional.ofNullable(config.getAzurePat())
                .orElseThrow(() -> new IllegalStateException("azure.pat not configured"));
        String token = ":" + pat; // username:password -> :PAT
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calls the Test Case endpoint:
     *   {projectUrl}/_apis/testplan/Plans/{planId}/Suites/{suiteId}/TestCase?api-version={apiVersion}
     * and converts the JSON array response into a list of TestCaseData objects.
     */
    public List<TestCaseData> loadTestCasesFromConfiguredSuite() throws Exception {
        int planId = config.getAzurePlanId();
        int suiteId = config.getAzureSuiteId();
        if (planId <= 0 || suiteId <= 0) {
            throw new IllegalArgumentException("PlanId or SuiteId missing/invalid in config");
        }

        String url = String.format(
                "%s/_apis/testplan/Plans/%d/Suites/%d/TestCase?api-version=%s",
                config.getAzureProjectUrl(),
                planId,
                suiteId,
                config.getAzureApiVersion()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get test cases from suite: HTTP " + response.statusCode()
                    + " body: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        if (!root.isArray()) {
            throw new RuntimeException("Expected JSON array from testCase endpoint");
        }

        List<TestCaseData> testCases = new ArrayList<>();
        for (JsonNode item : root) {
            TestCaseData data = parseTestCaseItem(item);
            if (data != null) {
                testCases.add(data);
            }
        }

        return testCases;
    }

    /**
     * Parse a single array element (one test case in the suite)
     * into TestCaseData, including multiple steps.
     */
    private TestCaseData parseTestCaseItem(JsonNode item) throws Exception {
        JsonNode workItem = item.path("workItem");
        if (workItem.isMissingNode()) {
            // Unexpected, but we skip if no workItem
            return null;
        }

        int id = workItem.path("id").asInt();
        String workItemName = workItem.path("name").asText("");

        // Suite / plan / project info
        int order = item.path("order").asInt();

        JsonNode planNode = item.path("testPlan");
        int planId = planNode.path("id").asInt(0);
        String planName = planNode.path("name").asText(null);

        JsonNode suiteNode = item.path("testSuite");
        int suiteId = suiteNode.path("id").asInt(0);
        String suiteName = suiteNode.path("name").asText(null);

        JsonNode projectNode = item.path("project");
        String projectName = projectNode.path("name").asText(null);

        // Work item fields (System.State, Priority, Steps, DataSource, etc.)
        String state = null;
        Integer priority = null;
        String stepsXml = null;
        String dataSourceXml = null;

        JsonNode fieldsArray = workItem.withArray("workItemFields");
        for (JsonNode fieldNode : fieldsArray) {
            // Pattern 1: { "fieldName": "System.Title", "value": "..." }
            if (fieldNode.has("fieldName")) {
                String fieldName = fieldNode.path("fieldName").asText();
                JsonNode valueNode = fieldNode.get("value");

                if ("System.State".equals(fieldName) && valueNode != null) {
                    state = valueNode.asText(null);
                }
                if ("Microsoft.VSTS.Common.Priority".equals(fieldName) && valueNode != null) {
                    priority = valueNode.isNumber() ? valueNode.intValue() : null;
                }
                if ("Microsoft.VSTS.TCM.Steps".equals(fieldName) && valueNode != null) {
                    stepsXml = valueNode.asText(null);
                }
                if ("Microsoft.VSTS.TCM.LocalDataSource".equals(fieldName) && valueNode != null) {
                    dataSourceXml = valueNode.asText(null);
                }
            }

            // Pattern 2 (like in your sample): { "Microsoft.VSTS.TCM.Steps": "<steps ...>" }
            if (fieldNode.has("Microsoft.VSTS.TCM.Steps")) {
                stepsXml = fieldNode.get("Microsoft.VSTS.TCM.Steps").asText(null);
            }
            if (fieldNode.has("Microsoft.VSTS.TCM.LocalDataSource")) {
                dataSourceXml = fieldNode.get("Microsoft.VSTS.TCM.LocalDataSource").asText(null);
            }
        }

        // Title: prefer System.Title if present, else workItem.name
        String title = workItemName; // default
        for (JsonNode fieldNode : fieldsArray) {
            if (fieldNode.has("fieldName")
                    && "System.Title".equals(fieldNode.path("fieldName").asText())
                    && fieldNode.has("value")) {
                title = fieldNode.get("value").asText(workItemName);
                break;
            }
        }

        // Build TestCaseData
        TestCaseData data = new TestCaseData(
                id,
                title,
                order,
                planId,
                planName,
                suiteId,
                suiteName,
                projectName,
                state,
                priority
        );

        // Point assignments -> configurations
        JsonNode pointAssignments = item.withArray("pointAssignments");
        for (JsonNode pa : pointAssignments) {
            String cfgName = pa.path("configurationName").asText(null);
            data.addConfiguration(cfgName);
        }

        // Steps
        if (stepsXml != null && !stepsXml.isEmpty()) {
            parseStepsXml(stepsXml, data);
        }

        // Data source (if present)
        if (dataSourceXml != null && !dataSourceXml.isEmpty()) {
            parseDataSourceXml(dataSourceXml, data);
        }

        return data;
    }

    /**
     * Parse <steps> XML into TestStep objects and add to TestCaseData.
     */
    private void parseStepsXml(String stepsXml, TestCaseData data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        ByteArrayInputStream is = new ByteArrayInputStream(stepsXml.getBytes(StandardCharsets.UTF_8));
        Document doc = factory.newDocumentBuilder().parse(is);
        NodeList stepNodes = doc.getElementsByTagName("step");

        for (int i = 0; i < stepNodes.getLength(); i++) {
            Element stepEl = (Element) stepNodes.item(i);
            int id = Integer.parseInt(stepEl.getAttribute("id"));
            String type = stepEl.getAttribute("type");

            NodeList paramNodes = stepEl.getElementsByTagName("parameterizedString");
            String action = "";
            String expected = "";
            if (paramNodes.getLength() > 0) {
                action = paramNodes.item(0).getTextContent();
            }
            if (paramNodes.getLength() > 1) {
                expected = paramNodes.item(1).getTextContent();
            }

            // Unescape HTML entities (&lt;DIV&gt; -> <DIV>, etc.)
            action = StringEscapeUtils.unescapeHtml4(action).trim();
            expected = StringEscapeUtils.unescapeHtml4(expected).trim();

            data.addStep(new TestStep(id, type, action, expected));
        }
    }

    /**
     * Parse LocalDataSource XML into list of data rows (parameter -> value).
     * Expected structure like:
     * <NewDataSet>
     *   <Table1>
     *     <param1>value1</param1>
     *     <param2>value2</param2>
     *   </Table1>
     *   ...
     * </NewDataSet>
     */
    private void parseDataSourceXml(String dataSourceXml, TestCaseData data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        ByteArrayInputStream is = new ByteArrayInputStream(dataSourceXml.getBytes(StandardCharsets.UTF_8));
        Document doc = factory.newDocumentBuilder().parse(is);

        NodeList tableNodes = doc.getElementsByTagName("Table1");
        for (int i = 0; i < tableNodes.getLength(); i++) {
            Element table = (Element) tableNodes.item(i);
            Map<String, String> row = new LinkedHashMap<>();
            NodeList children = table.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node n = children.item(j);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    String name = e.getTagName();
                    String value = e.getTextContent();
                    row.put(name, value);
                }
            }
            if (!row.isEmpty()) {
                data.addDataRow(row);
            }
        }
    }
}
