package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfluenceSpaceService {
    private final AtlassianConnectionService atlassian;
    private final ObjectMapper objectMapper;

    public ConfluenceSpaceService(AtlassianConnectionService atlassian, ObjectMapper objectMapper) {
        this.atlassian = atlassian;
        this.objectMapper = objectMapper;
    }

    public List<SpaceOption> search(IntegrationConfig config, String text) {
        if (config == null || !config.complete() || text == null || text.trim().length() < 2) return List.of();
        try {
            String query = text.trim();
            String path = "/api/v2/spaces?limit=100";
            HttpResponse<String> response = atlassian.confluenceGet(config, path);
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Confluence respondió HTTP " + response.statusCode() + " al consultar espacios.");
            JsonNode results = objectMapper.readTree(response.body()).path("results");
            List<SpaceOption> matches = new ArrayList<>();
            if (results.isArray()) {
                String needle = query.toLowerCase();
                for (JsonNode item : results) {
                    String id = item.path("id").asText("");
                    String key = item.path("key").asText("");
                    String name = item.path("name").asText("");
                    if ((key + " " + name).toLowerCase().contains(needle)) {
                        matches.add(new SpaceOption(id, key, name));
                        if (matches.size() == 5) break;
                    }
                }
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

    public record SpaceOption(String id, String key, String name) { }
}
