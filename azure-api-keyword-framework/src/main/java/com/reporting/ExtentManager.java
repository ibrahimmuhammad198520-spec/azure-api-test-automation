package com.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class ExtentManager {

    private static ExtentReports extent;

    public static synchronized ExtentReports createInstance(String reportPath) {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        return extent;
    }

    public static ExtentReports getExtent() {
        if (extent == null) {
            throw new IllegalStateException("ExtentReports not initialized");
        }
        return extent;
    }
}
