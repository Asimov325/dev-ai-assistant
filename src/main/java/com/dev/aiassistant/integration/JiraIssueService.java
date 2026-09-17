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
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public List<JiraIssueOption> search(IntegrationConfig config, String text) {
        if (config == null || !config.complete() || text == null || text.trim().length() < 2) return List.of();
        try {
            String value = text.trim();
            String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
            String project = config.context() == null || config.context().isBlank() ? "" : "project = \"" + config.context().trim().replace("\"", "\\\"") + "\" AND ";
            String jql = project + "(key = \"" + escaped + "\" OR summary ~ \"" + escaped + "*\") ORDER BY updated DESC";
            String url = config.url().replaceAll("/+$", "") + "/rest/api/3/search/jql?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&maxResults=5&fields=summary,status";
            String auth = Base64.getEncoder().encodeToString((config.username() + ":" + config.secret()).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("Authorization", "Basic " + auth).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

    private String unescape(String value) { return value.replace("\\\"", "\"").replace("\\n", " ").replace("\\\\", "\\"); }
    public record JiraIssueOption(String key, String summary, String status) { }
}
