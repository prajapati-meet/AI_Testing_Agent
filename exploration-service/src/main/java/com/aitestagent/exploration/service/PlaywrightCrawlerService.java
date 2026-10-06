package com.aitestagent.exploration.service;

import com.aitestagent.exploration.entity.DiscoveredPage;
import com.aitestagent.exploration.entity.Exploration;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless service that uses Playwright to crawl a web application and extract
 * structured information from each page.
 *
 * <p>Crawling strategy:</p>
 * <ul>
 *   <li>Starts at the provided targetUrl (depth 0).</li>
 *   <li>Follows same-origin internal {@code <a href>} links up to {@code MAX_DEPTH}.</li>
 *   <li>Each page is scraped for: title, links, buttons, inputs, and forms.</li>
 *   <li>All structured data is serialised as JSON strings so they map directly to the
 *       {@code DiscoveredPage} entity columns.</li>
 * </ul>
 */
@Service
public class PlaywrightCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightCrawlerService.class);

    /** Maximum BFS crawl depth from the seed URL. */
    private static final int MAX_DEPTH = 2;

    /** Maximum number of pages to crawl per exploration to avoid runaway crawls. */
    private static final int MAX_PAGES = 20;

    /** Milliseconds to wait for navigation to finish. */
    private static final int NAV_TIMEOUT_MS = 15_000;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Crawls the target application starting from {@code exploration.getTargetUrl()}.
     *
     * @param exploration the parent exploration entity (used for setting the ManyToOne FK)
     * @return ordered list of {@link DiscoveredPage} objects ready to be persisted
     */
    public List<DiscoveredPage> crawl(Exploration exploration) {
        String seedUrl = normaliseUrl(exploration.getTargetUrl());
        String origin = extractOrigin(seedUrl);

        List<DiscoveredPage> results = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            try (BrowserContext context = browser.newContext()) {
                // BFS frontier: map from url → depth
                Map<String, Integer> frontier = new LinkedHashMap<>();
                Set<String> visited = new LinkedHashSet<>();

                frontier.put(seedUrl, 0);

                while (!frontier.isEmpty() && results.size() < MAX_PAGES) {
                    Map.Entry<String, Integer> entry = frontier.entrySet().iterator().next();
                    frontier.remove(entry.getKey());

                    String url = entry.getKey();
                    int depth = entry.getValue();

                    if (visited.contains(url)) {
                        continue;
                    }
                    visited.add(url);

                    log.info("[Crawl] Visiting [depth={}] {}", depth, url);

                    DiscoveredPage page = scrapePage(context, url, exploration);
                    if (page == null) {
                        continue; // navigation error on this URL — skip
                    }
                    results.add(page);

                    // Enqueue unvisited internal links if we haven't hit max depth
                    if (depth < MAX_DEPTH) {
                        List<String> links = parseJsonStringArray(page.getDiscoveredLinks());
                        for (String link : links) {
                            String absolute = toAbsolute(link, origin);
                            if (absolute != null
                                    && isSameOrigin(absolute, origin)
                                    && !visited.contains(absolute)
                                    && !frontier.containsKey(absolute)) {
                                frontier.put(absolute, depth + 1);
                            }
                        }
                    }
                }
            }
        }

        log.info("[Crawl] Finished. {} pages discovered.", results.size());
        return results;
    }

    /**
     * Generates a plain-text navigation flow summary from the list of discovered pages.
     * This summary is stored on the {@link Exploration} entity and is what gets sent to the LLM.
     *
     * <p>Example output:</p>
     * <pre>
     * Home (http://localhost:3000/) → links to → Login (/login), Products (/products)
     * Login (http://localhost:3000/login) → contains login form with email + password inputs and [Login] button
     * Products (http://localhost:3000/products) → links to → Cart (/cart)
     * </pre>
     */
    public String buildNavigationFlowSummary(List<DiscoveredPage> pages) {
        StringBuilder sb = new StringBuilder();

        for (DiscoveredPage page : pages) {
            String title = (page.getPageTitle() != null && !page.getPageTitle().isBlank())
                    ? page.getPageTitle()
                    : page.getPageUrl();

            sb.append(title).append(" (").append(page.getPageUrl()).append(")\n");

            // Links
            List<String> links = parseJsonStringArray(page.getDiscoveredLinks());
            if (!links.isEmpty()) {
                sb.append("  → links to → ").append(String.join(", ", links)).append("\n");
            }

            // Buttons
            List<String> buttonTexts = extractJsonFieldValues(page.getButtons(), "text");
            if (!buttonTexts.isEmpty()) {
                sb.append("  → buttons: [").append(String.join("] [", buttonTexts)).append("]\n");
            }

            // Inputs
            List<String> inputDescriptions = buildInputDescriptions(page.getInputs());
            if (!inputDescriptions.isEmpty()) {
                sb.append("  → inputs: ").append(String.join(", ", inputDescriptions)).append("\n");
            }

            // Forms
            List<String> formDescriptions = buildFormDescriptions(page.getForms());
            if (!formDescriptions.isEmpty()) {
                sb.append("  → forms: ").append(String.join("; ", formDescriptions)).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Private — Page scraping
    // -------------------------------------------------------------------------

    private DiscoveredPage scrapePage(BrowserContext context, String url, Exploration exploration) {
        Page pw = context.newPage();
        try {
            Page.NavigateOptions opts = new Page.NavigateOptions()
                    .setTimeout(NAV_TIMEOUT_MS)
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED);

            pw.navigate(url, opts);

            String title = pw.title();
            String linksJson = extractLinks(pw);
            String buttonsJson = extractButtons(pw);
            String inputsJson = extractInputs(pw);
            String formsJson = extractForms(pw);

            DiscoveredPage dp = new DiscoveredPage();
            dp.setExploration(exploration);
            dp.setPageUrl(url);
            dp.setPageTitle(title);
            dp.setDiscoveredLinks(linksJson);
            dp.setButtons(buttonsJson);
            dp.setInputs(inputsJson);
            dp.setForms(formsJson);
            dp.setNavigationFlow(buildPageFlow(title, url, linksJson, buttonsJson, inputsJson));

            return dp;
        } catch (Exception e) {
            log.warn("[Crawl] Failed to scrape {}: {}", url, e.getMessage());
            return null;
        } finally {
            try {
                pw.close();
            } catch (Exception ignored) {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private — Element extraction (each returns a JSON string)
    // -------------------------------------------------------------------------

    /** Extracts all {@code <a href>} values from the page as a JSON array string. */
    private String extractLinks(Page page) {
        List<String> hrefs = new ArrayList<>();
        List<ElementHandle> anchors = page.querySelectorAll("a[href]");
        for (ElementHandle a : anchors) {
            String href = a.getAttribute("href");
            if (href != null && !href.isBlank()
                    && !href.startsWith("javascript:")
                    && !href.startsWith("mailto:")
                    && !href.startsWith("tel:")
                    && !href.startsWith("#")) {
                hrefs.add(escapeJson(href.trim()));
            }
        }
        return toJsonArray(hrefs);
    }

    /**
     * Extracts all {@code <button>} elements (and {@code <input type="submit|button|reset">})
     * as a JSON array of {@code {text, selector}} objects.
     */
    private String extractButtons(Page page) {
        List<String> items = new ArrayList<>();

        // Standard <button> elements
        List<ElementHandle> buttons = page.querySelectorAll("button");
        for (ElementHandle btn : buttons) {
            String text = btn.innerText().trim();
            String id = btn.getAttribute("id");
            String cls = btn.getAttribute("class");
            String selector = buildSelector("button", id, cls);
            items.add(toJsonObject("text", text, "selector", selector));
        }

        // Input buttons
        List<ElementHandle> inputBtns = page.querySelectorAll("input[type='submit'], input[type='button'], input[type='reset']");
        for (ElementHandle inp : inputBtns) {
            String text = inp.getAttribute("value");
            if (text == null) text = inp.getAttribute("type");
            String id = inp.getAttribute("id");
            String cls = inp.getAttribute("class");
            String selector = buildSelector("input", id, cls);
            items.add(toJsonObject("text", text != null ? text : "", "selector", selector));
        }

        return "[" + String.join(",", items) + "]";
    }

    /**
     * Extracts all {@code <input>} elements (excluding submit/button/hidden) as a JSON array
     * of {@code {id, name, type, placeholder}} objects.
     */
    private String extractInputs(Page page) {
        List<String> items = new ArrayList<>();
        List<ElementHandle> inputs = page.querySelectorAll(
                "input:not([type='submit']):not([type='button']):not([type='reset']):not([type='hidden'])");
        for (ElementHandle inp : inputs) {
            String id = nvl(inp.getAttribute("id"));
            String name = nvl(inp.getAttribute("name"));
            String type = nvl(inp.getAttribute("type"), "text");
            String placeholder = nvl(inp.getAttribute("placeholder"));
            items.add(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"placeholder\":\"%s\"}",
                    escapeJson(id), escapeJson(name), escapeJson(type), escapeJson(placeholder)));
        }
        return "[" + String.join(",", items) + "]";
    }

    /**
     * Extracts all {@code <form>} elements as a JSON array of
     * {@code {formId, action, method, fields[]}} objects.
     */
    private String extractForms(Page page) {
        List<String> formItems = new ArrayList<>();
        List<ElementHandle> forms = page.querySelectorAll("form");
        for (ElementHandle form : forms) {
            String formId = nvl(form.getAttribute("id"));
            String action = nvl(form.getAttribute("action"));
            String method = nvl(form.getAttribute("method"), "get");

            List<String> fields = new ArrayList<>();
            List<ElementHandle> formInputs = form.querySelectorAll("input, select, textarea");
            for (ElementHandle fi : formInputs) {
                String name = nvl(fi.getAttribute("name"));
                String type = nvl(fi.getAttribute("type"), "text");
                String placeholder = nvl(fi.getAttribute("placeholder"));
                fields.add(String.format(
                        "{\"name\":\"%s\",\"type\":\"%s\",\"placeholder\":\"%s\"}",
                        escapeJson(name), escapeJson(type), escapeJson(placeholder)));
            }

            formItems.add(String.format(
                    "{\"formId\":\"%s\",\"action\":\"%s\",\"method\":\"%s\",\"fields\":[%s]}",
                    escapeJson(formId), escapeJson(action), escapeJson(method),
                    String.join(",", fields)));
        }
        return "[" + String.join(",", formItems) + "]";
    }

    // -------------------------------------------------------------------------
    // Private — Navigation flow per page
    // -------------------------------------------------------------------------

    private String buildPageFlow(String title, String url, String linksJson,
                                  String buttonsJson, String inputsJson) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" (").append(url).append(")");

        List<String> links = parseJsonStringArray(linksJson);
        if (!links.isEmpty()) {
            sb.append(" → links to → ").append(String.join(", ", links));
        }
        List<String> btns = extractJsonFieldValues(buttonsJson, "text");
        if (!btns.isEmpty()) {
            sb.append(" → buttons: [").append(String.join("] [", btns)).append("]");
        }
        List<String> inputDescs = buildInputDescriptions(inputsJson);
        if (!inputDescs.isEmpty()) {
            sb.append(" → inputs: ").append(String.join(", ", inputDescs));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private — JSON helpers (no external dependency — keep it lightweight)
    // -------------------------------------------------------------------------

    private String toJsonArray(List<String> items) {
        if (items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append("\"").append(escapeJson(items.get(i))).append("\"");
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonObject(String k1, String v1, String k2, String v2) {
        return String.format("{\"" + k1 + "\":\"%s\",\"" + k2 + "\":\"%s\"}",
                escapeJson(v1), escapeJson(v2));
    }

    /**
     * Very lightweight JSON string array parser — handles the arrays we build above.
     * Not a full JSON parser; only parses arrays of quoted strings.
     */
    private List<String> parseJsonStringArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return result;
        // Strip outer brackets
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        // Split by "," outside quotes — simplified: works for our generated JSON
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                String token = current.toString().trim().replaceAll("^\"|\"$", "");
                if (!token.isBlank()) result.add(token);
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        String last = current.toString().trim().replaceAll("^\"|\"$", "");
        if (!last.isBlank()) result.add(last);
        return result;
    }

    /**
     * Extracts values of a specific key from a JSON array of objects.
     * e.g. extractJsonFieldValues([{"text":"Login","selector":"button"}], "text") → ["Login"]
     */
    private List<String> extractJsonFieldValues(String json, String key) {
        List<String> values = new ArrayList<>();
        if (json == null || json.isBlank() || json.equals("[]")) return values;
        String pattern = "\"" + key + "\":\"";
        int idx = 0;
        while ((idx = json.indexOf(pattern, idx)) != -1) {
            idx += pattern.length();
            int end = json.indexOf("\"", idx);
            if (end == -1) break;
            values.add(json.substring(idx, end));
            idx = end + 1;
        }
        return values;
    }

    private List<String> buildInputDescriptions(String inputsJson) {
        List<String> result = new ArrayList<>();
        if (inputsJson == null || inputsJson.isBlank() || inputsJson.equals("[]")) return result;
        List<String> names = extractJsonFieldValues(inputsJson, "name");
        List<String> types = extractJsonFieldValues(inputsJson, "type");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            String type = (i < types.size()) ? types.get(i) : "text";
            if (!name.isBlank()) {
                result.add(name + " (" + type + ")");
            }
        }
        return result;
    }

    private List<String> buildFormDescriptions(String formsJson) {
        List<String> result = new ArrayList<>();
        if (formsJson == null || formsJson.isBlank() || formsJson.equals("[]")) return result;
        List<String> actions = extractJsonFieldValues(formsJson, "action");
        List<String> formIds = extractJsonFieldValues(formsJson, "formId");
        for (int i = 0; i < Math.max(actions.size(), formIds.size()); i++) {
            String id = (i < formIds.size()) ? formIds.get(i) : "";
            String action = (i < actions.size()) ? actions.get(i) : "";
            result.add("form" + (id.isBlank() ? "" : "#" + id)
                    + (action.isBlank() ? "" : " action=" + action));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private — URL helpers
    // -------------------------------------------------------------------------

    private String normaliseUrl(String url) {
        if (url == null) return "http://localhost:3000";
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        return u;
    }

    private String extractOrigin(String url) {
        try {
            URI uri = new URI(url);
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port > 0 ? ":" + port : "");
        } catch (Exception e) {
            return url;
        }
    }

    private boolean isSameOrigin(String url, String origin) {
        return url.startsWith(origin);
    }

    private String toAbsolute(String href, String origin) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("/")) return origin + href;
        return null; // relative paths without leading slash — skip for simplicity
    }

    // -------------------------------------------------------------------------
    // Private — Misc
    // -------------------------------------------------------------------------

    private String buildSelector(String tag, String id, String cls) {
        if (id != null && !id.isBlank()) return tag + "#" + id.trim();
        if (cls != null && !cls.isBlank()) return tag + "." + cls.trim().split("\\s+")[0];
        return tag;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String nvl(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
