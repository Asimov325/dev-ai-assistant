package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfluenceSpaceService {
    private static final int PAGE_LIMIT = 100;
    private static final int MAX_RESULTS = 5;

    private final AtlassianConnectionService atlassian;
    private final ObjectMapper objectMapper;

    public ConfluenceSpaceService(AtlassianConnectionService atlassian, ObjectMapper objectMapper) {
        this.atlassian = atlassian;
        this.objectMapper = objectMapper;
    }

    public List<SpaceOption> search(IntegrationConfig config, String text) {
        if (config == null || !config.complete() || text == null || text.trim().length() < 2) return List.of();
        try {
            String needle = text.trim().toLowerCase();
            String path = "/api/v2/spaces?limit=" + PAGE_LIMIT;
            List<SpaceOption> matches = new ArrayList<>();

            while (path != null && matches.size() < MAX_RESULTS) {
                HttpResponse<String> response = atlassian.confluenceGet(config, path);
                if (response.statusCode() < 200 || response.statusCode() >= 300)
                    throw new IllegalStateException("Confluence respondió HTTP " + response.statusCode() + " al consultar espacios.");

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode item : results) {
                        String id = item.path("id").asText("");
                        String key = item.path("key").asText("");
                        String name = item.path("name").asText("");
                        if ((key + " " + name).toLowerCase().contains(needle)) {
                            matches.add(new SpaceOption(id, key, name));
                            if (matches.size() == MAX_RESULTS) break;
                        }
                    }
                }
                path = matches.size() < MAX_RESULTS ? nextPath(root) : null;
            }
            return matches;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La búsqueda de espacios Confluence fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible consultar los espacios accesibles de Confluence.", ex);
        }
    }

    private String nextPath(JsonNode root) {
        String next = root.path("_links").path("next").asText("");
        if (next.isBlank()) return null;
        URI uri = URI.create(next);
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        int wiki = path.indexOf("/wiki");
        if (wiki >= 0) path = path.substring(wiki + "/wiki".length());
        return query == null ? path : path + "?" + query;
    }

    public record SpaceOption(String id, String key, String name) {
    }
}
