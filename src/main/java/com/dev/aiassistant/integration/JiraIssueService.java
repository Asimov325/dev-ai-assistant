package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JiraIssueService {
    private static final Pattern ISSUE = Pattern.compile("\\{[^{}]*\\\"key\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^{}]*\\\"fields\\\"\\s*:\\s*\\{(.*?)}}", Pattern.DOTALL);
    private static final Pattern SUMMARY = Pattern.compile("\\\"summary\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern STATUS = Pattern.compile("\\\"status\\\"\\s*:\\s*\\{.*?\\\"name\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
    private static final Pattern ISSUE_TYPE = Pattern.compile("\\\"issuetype\\\"\\s*:\\s*\\{.*?\\\"name\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public List<JiraIssueOption> search(IntegrationConfig config, String text) {
        if (config == null || !config.complete() || text == null || text.trim().length() < 2) return List.of();
        try {
            String value = text.trim();
            String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
            String project = config.context() == null || config.context().isBlank() ? "" : "project = \"" + config.context().trim().replace("\"", "\\\"") + "\" AND ";
            String jql = project + "(key = \"" + escaped + "\" OR summary ~ \"" + escaped + "*\") ORDER BY updated DESC";
            String url = config.url().replaceAll("/+$", "") + "/rest/api/3/search/jql?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&maxResults=5&fields=summary,status";
            HttpResponse<String> response = send(config, url);
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Jira respondió HTTP " + response.statusCode() + ".");
            return parse(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La búsqueda Jira fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible buscar requerimientos en Jira.", ex);
        }
    }

    public JiraIssueContext getContext(IntegrationConfig config, String issueKey) {
        if (config == null || !config.complete()) throw new IllegalStateException("Jira no está configurado.");
        if (issueKey == null || issueKey.isBlank()) throw new IllegalArgumentException("El requerimiento Jira es obligatorio.");
        try {
            String key = URLEncoder.encode(issueKey.trim(), StandardCharsets.UTF_8);
            String url = config.url().replaceAll("/+$", "") + "/rest/api/3/issue/" + key + "?fields=summary,status,issuetype,description";
            HttpResponse<String> response = send(config, url);
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Jira respondió HTTP " + response.statusCode() + " al recuperar el requerimiento.");
            String body = response.body() == null ? "" : response.body();
            String summary = extract(SUMMARY, body);
            String status = extract(STATUS, body);
            String issueType = extract(ISSUE_TYPE, body);
            String description = extractDescriptionText(body);
            return new JiraIssueContext(issueKey.trim(), summary, status, issueType, description);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta Jira fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible recuperar el contexto del requerimiento Jira.", ex);
        }
    }

    private HttpResponse<String> send(IntegrationConfig config, String url) throws Exception {
        String auth = Base64.getEncoder().encodeToString((config.username() + ":" + config.secret()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json").header("Authorization", "Basic " + auth).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<JiraIssueOption> parse(String body) {
        List<JiraIssueOption> result = new ArrayList<>();
        Matcher issues = ISSUE.matcher(body == null ? "" : body);
        while (issues.find() && result.size() < 5) {
            String fields = issues.group(2);
            Matcher summary = SUMMARY.matcher(fields);
            Matcher status = STATUS.matcher(fields);
            result.add(new JiraIssueOption(issues.group(1), summary.find() ? unescape(summary.group(1)) : "", status.find() ? unescape(status.group(1)) : ""));
        }
        return result;
    }

    private String extract(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? unescape(matcher.group(1)) : "";
    }

    private String extractDescriptionText(String body) {
        int start = body.indexOf("\"description\"");
        if (start < 0) return "";
        int end = body.indexOf("\"environment\"", start);
        String part = end > start ? body.substring(start, end) : body.substring(start);
        Matcher text = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(part);
        StringBuilder value = new StringBuilder();
        while (text.find()) {
            if (!value.isEmpty()) value.append('\n');
            value.append(unescape(text.group(1)));
        }
        return value.toString();
    }

    private String unescape(String value) { return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "").replace("\\t", " ").replace("\\\\", "\\"); }
    public record JiraIssueOption(String key, String summary, String status) { }
    public record JiraIssueContext(String key, String summary, String status, String issueType, String description) { }
}
