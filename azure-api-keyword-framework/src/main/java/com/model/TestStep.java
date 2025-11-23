package com.model;

public class TestStep {
    private final int id;
    private final String type;
    private final String action;
    private final String expected;

    public TestStep(int id, String type, String action, String expected) {
        this.id = id;
        this.type = type;
        this.action = action;
        this.expected = expected;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public String getExpected() {
        return expected;
    }
}
