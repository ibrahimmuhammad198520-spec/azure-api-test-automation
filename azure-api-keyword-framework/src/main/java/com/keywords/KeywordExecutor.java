package com.keywords;

import com.aventstack.extentreports.Status;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class KeywordExecutor {

    private final Map<String, KeywordHandler> handlers = new HashMap<>();

    public void register(String keyword, KeywordHandler handler) {
        handlers.put(keyword.toUpperCase(Locale.ROOT), handler);
    }

    public void execute(String keyword, KeywordContext ctx) {
        if (keyword == null || keyword.isBlank()) {
            ctx.getExtentTest().log(Status.WARNING,
                    "No keyword found for step ID " + ctx.getStep().getId());
            return;
        }

        KeywordHandler handler = handlers.get(keyword.toUpperCase(Locale.ROOT));
        if (handler == null) {
            ctx.getExtentTest().log(Status.WARNING,
                    "No handler registered for keyword '" + keyword +
                            "'. Step will be marked as skipped.");
            return;
        }

        try {
            handler.execute(ctx);
            ctx.getExtentTest().log(Status.PASS,
                    "Keyword '" + keyword + "' executed successfully for step ID " +
                            ctx.getStep().getId());
        } catch (Exception e) {
            ctx.getExtentTest().log(Status.FAIL,
                    "Keyword '" + keyword + "' failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
