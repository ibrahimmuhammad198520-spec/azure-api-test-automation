package com.parser;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AzureStepParser {

    public static class TestStep {
        public int id;
        public String action;
        public String expectedResult;

        public TestStep(int id, String action, String expectedResult) {
            this.id = id;
            this.action = action;
            this.expectedResult = expectedResult;
        }

        @Override
        public String toString() {
            return "Step " + id + "\n" +
                   "  Action         : " + action + "\n" +
                   "  Expected Result: " + expectedResult + "\n";
        }
    }

    /**
     * Main entry for testing.
     */
    public static void main(String[] args) throws Exception {
        String xml =
                "<steps id=\"0\" last=\"10\">" +
                    "<step id=\"2\" type=\"ActionStep\">" +
                        "<parameterizedString isformatted=\"true\">" +
                        "&lt;DIV&gt;&lt;P&gt;Customer goes to an OFFUS ATM supporting NFC&lt;/P&gt;&lt;/DIV&gt;" +
                        "</parameterizedString>" +
                        "<parameterizedString isformatted=\"true\">" +
                        "&lt;DIV&gt;&lt;P&gt;&lt;BR/&gt;&lt;/P&gt;&lt;/DIV&gt;" +
                        "</parameterizedString>" +
                        "<description/>" +
                    "</step>" +
                    "<step id=\"10\" type=\"ValidateStep\">" +
                        "<parameterizedString isformatted=\"true\">" +
                        "&lt;DIV&gt;&lt;P&gt;Test Automation Step&lt;/P&gt;&lt;/DIV&gt;" +
                        "</parameterizedString>" +
                        "<parameterizedString isformatted=\"true\">" +
                        "&lt;P&gt;{\\n\\t\\\"header\\\": \\\"ISO026000070\\\"}\\n&lt;/P&gt;" +
                        "</parameterizedString>" +
                        "<description/>" +
                    "</step>" +
                "</steps>";

        List<TestStep> steps = parseSteps(xml);
        for (TestStep s : steps) {
            System.out.println(s);
        }
    }

    /**
     * Parse Azure DevOps "steps" XML and return plain-text step id, action, expected result.
     */
    public static List<TestStep> parseSteps(String xml) throws Exception {
        List<TestStep> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Element root = doc.getDocumentElement(); // <steps>
        NodeList stepNodes = root.getElementsByTagName("step");

        for (int i = 0; i < stepNodes.getLength(); i++) {
            Element stepEl = (Element) stepNodes.item(i);

            // Step id
            String idStr = stepEl.getAttribute("id");
            int id = -1;
            try {
                id = Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                // ignore, keep -1 if not numeric
            }

            // All parameterizedString nodes under this step
            NodeList paramNodes = stepEl.getElementsByTagName("parameterizedString");

            String actionText = "";
            String expectedText = "";

            if (paramNodes.getLength() >= 1) {
                actionText = plainTextFromParameterizedNode((Element) paramNodes.item(0));
            }
            if (paramNodes.getLength() >= 2) {
                expectedText = plainTextFromParameterizedNode((Element) paramNodes.item(1));
            }

            result.add(new TestStep(id, actionText, expectedText));
        }

        return result;
    }

    /**
     * Convert one <parameterizedString> node into plain text:
     *  - unescape XML/HTML entities (&lt;DIV&gt; -> <DIV>)
     *  - drop HTML tags, keep only text
     */
    private static String plainTextFromParameterizedNode(Element el) {
        String raw = el.getTextContent();          // still contains &lt;DIV&gt;&lt;P&gt;...
        String html = unescapeBasicEntities(raw);  // now real <DIV><P>...</P></DIV>
        return htmlToPlainText(html);
    }

    /**
     * Very small HTML entity unescaper for the common entities used in Azure DevOps steps.
     */
    private static String unescapeBasicEntities(String s) {
        if (s == null) return "";
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    /**
     * Very simple HTML-to-text conversion:
     *  - convert <br> to newline
     *  - remove all other tags
     *  - normalize whitespace
     */
    private static String htmlToPlainText(String html) {
        if (html == null) return "";

        // Convert different forms of <br> to newline
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");

        // Remove all remaining tags
        text = text.replaceAll("<[^>]+>", " ");

        // Convert multiple whitespace to single space/newline friendly
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        text = text.replaceAll("\\n\\s+", "\n");

        return text.trim();
    }
}
