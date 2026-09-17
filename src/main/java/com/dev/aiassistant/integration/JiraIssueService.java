package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class JiraIssueService {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;

    public JiraIssueService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<JiraIssueOption> search(IntegrationConfig config, String text) {
        if (config == null || !config.complete() || text == null || text.trim().length() < 2) return List.of();
        try {
            String value = text.trim();
            String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
            String project = config.context() == null || config.context().isBlank()
                    ? ""
                    : "project = \"" + config.context().trim().replace("\"", "\\\"") + "\" AND ";
            String jql = project + "(key = \"" + escaped + "\" OR summary ~ \"" + escaped + "*\") ORDER BY updated DESC";
            String url = config.url().replaceAll("/+$", "") + "/rest/api/3/search/jql?jql="
                    + URLEncoder.encode(jql, StandardCharsets.UTF_8) + "&maxResults=5&fields=summary,status";
            HttpResponse<String> response = send(config, url);
            ensureSuccess(response, "Jira respondió HTTP %d.");
            return parseSearch(response.body());
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
            String url = config.url().replaceAll("/+$", "") + "/rest/api/3/issue/" + key
                    + "?fields=summary,status,issuetype,description";
            HttpResponse<String> response = send(config, url);
            ensureSuccess(response, "Jira respondió HTTP %d al recuperar el requerimiento.");

            JsonNode fields = objectMapper.readTree(response.body()).path("fields");
            return new JiraIssueContext(
                    issueKey.trim(),
                    text(fields.path("summary")),
                    text(fields.path("status").path("name")),
                    text(fields.path("issuetype").path("name")),
                    descriptionText(fields.path("description"))
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta Jira fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible recuperar el contexto del requerimiento Jira.", ex);
        }
    }

    private HttpResponse<String> send(IntegrationConfig config, String url) throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.secret()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + auth)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void ensureSuccess(HttpResponse<String> response, String message) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(message.formatted(response.statusCode()));
        }
    }

    private List<JiraIssueOption> parseSearch(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
        List<JiraIssueOption> result = new ArrayList<>();
        for (JsonNode issue : root.path("issues")) {
            if (result.size() >= 5) break;
            JsonNode fields = issue.path("fields");
            result.add(new JiraIssueOption(
                    text(issue.path("key")),
                    text(fields.path("summary")),
                    text(fields.path("status").path("name"))
            ));
        }
        return result;
    }

    private String descriptionText(JsonNode description) {
        if (description == null || description.isMissingNode() || description.isNull()) return "";
        if (description.isTextual()) return description.asText();
        List<String> fragments = new ArrayList<>();
        collectText(description, fragments);
        return String.join("\n", fragments);
    }

    private void collectText(JsonNode node, List<String> fragments) {
        if (node == null || node.isNull()) return;
        if (node.isObject()) {
            if ("text".equals(node.path("type").asText()) && node.path("text").isTextual()) {
                String value = node.path("text").asText().trim();
                if (!value.isBlank()) fragments.add(value);
                return;
            }
            node.elements().forEachRemaining(child -> collectText(child, fragments));
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> collectText(child, fragments));
        }
    }

    private String text(JsonNode node) {
        return node != null && node.isValueNode() && !node.isNull() ? node.asText("") : "";
    }

    public record JiraIssueOption(String key, String summary, String status) { }
    public record JiraIssueContext(String key, String summary, String status, String issueType, String description) { }
}
