package com.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCaseData {

    // Basic identity
    private int id;
    private String title;

    // Suite / plan / project metadata
    private int order;              // order inside the suite
    private int planId;
    private String planName;
    private int suiteId;
    private String suiteName;
    private String projectName;

    // Work item metadata
    private String state;           // System.State
    private Integer priority;       // Microsoft.VSTS.Common.Priority

    // Configurations from pointAssignments (e.g. "Windows 10, Chrome")
    private final List<String> configurations = new ArrayList<>();

    // Steps & data rows (keyword / data-driven)
    private final List<TestStep> steps = new ArrayList<>();
    private final List<Map<String, String>> dataRows = new ArrayList<>();

    // --- Constructors ---

    // Minimal constructor (kept for compatibility if you still want it)
    public TestCaseData(int id, String title) {
        this.id = id;
        this.title = title;
    }

    // Full constructor used by AzureDevOpsClient
    public TestCaseData(int id,
                        String title,
                        int order,
                        int planId,
                        String planName,
                        int suiteId,
                        String suiteName,
                        String projectName,
                        String state,
                        Integer priority) {
        this.id = id;
        this.title = title;
        this.order = order;
        this.planId = planId;
        this.planName = planName;
        this.suiteId = suiteId;
        this.suiteName = suiteName;
        this.projectName = projectName;
        this.state = state;
        this.priority = priority;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getOrder() {
        return order;
    }

    public int getPlanId() {
        return planId;
    }

    public String getPlanName() {
        return planName;
    }

    public int getSuiteId() {
        return suiteId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getState() {
        return state;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<String> getConfigurations() {
        return configurations;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    public List<Map<String, String>> getDataRows() {
        return dataRows;
    }

    // --- Mutators / helpers ---

    public void addConfiguration(String configurationName) {
        if (configurationName != null && !configurationName.isBlank()) {
            configurations.add(configurationName);
        }
    }

    public void addStep(TestStep step) {
        steps.add(step);
    }

    public void addDataRow(Map<String, String> row) {
        dataRows.add(row);
    }
}
