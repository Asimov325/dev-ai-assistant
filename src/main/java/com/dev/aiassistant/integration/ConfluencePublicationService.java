package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ConfluencePublicationService {
    private static final int PAGE_LIMIT = 100;
    private static final int MAX_CANDIDATES = 5;

    private final AtlassianConnectionService atlassian;
    private final ObjectMapper objectMapper;

    public ConfluencePublicationService(AtlassianConnectionService atlassian, ObjectMapper objectMapper) {
        this.atlassian = atlassian;
        this.objectMapper = objectMapper;
    }

    public List<ParentPage> findParentCandidates(IntegrationConfig config, String spaceId, String documentType) {
        requireConfigured(config, spaceId);
        try {
            List<ParentPage> candidates = new ArrayList<>();
            String path = "/api/v2/spaces/" + spaceId + "/pages?depth=all&status=current&limit=" + PAGE_LIMIT;
            while (path != null) {
                HttpResponse<String> response = atlassian.confluenceGet(config, path);
                ensureSuccess(response, "consultar páginas del espacio");
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.path("results");
                if (results.isArray()) {
                    for (JsonNode page : results) {
                        String title = page.path("title").asText("");
                        int score = parentScore(title, documentType);
                        if (score > 0) candidates.add(new ParentPage(page.path("id").asText(""), title, score));
                    }
                }
                path = nextPath(root);
            }
            return candidates.stream()
                    .sorted(Comparator.comparingInt(ParentPage::score).reversed().thenComparing(ParentPage::title))
                    .limit(MAX_CANDIDATES)
                    .toList();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La búsqueda de la página padre en Confluence fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible localizar la página padre en Confluence.", ex);
        }
    }

    public PublicationResult publish(IntegrationConfig config, String spaceId, String spaceKey, String spaceName,
                                     String parentId, String parentTitle, String title, String storageHtml) {
        requireConfigured(config, spaceId);
        if (blank(parentId)) throw new IllegalArgumentException("Selecciona la página padre de Confluence.");
        try {
            ExistingPage existing = findExistingChild(config, parentId, title);
            if (existing != null) {
                return new PublicationResult(false, true, existing.id(), title, spaceKey, spaceName,
                        parentId, parentTitle, pageUrl(config, existing.id()));
            }

            Map<String, Object> payload = Map.of(
                    "spaceId", spaceId,
                    "status", "current",
                    "title", title,
                    "parentId", parentId,
                    "body", Map.of("representation", "storage", "value", storageHtml)
            );
            HttpResponse<String> response = atlassian.confluencePost(config, "/api/v2/pages", objectMapper.writeValueAsString(payload));
            ensureSuccess(response, "crear el documento");
            JsonNode page = objectMapper.readTree(response.body());
            String pageId = page.path("id").asText("");
            if (pageId.isBlank()) throw new IllegalStateException("Confluence creó la página pero no devolvió su identificador.");
            return new PublicationResult(true, false, pageId, title, spaceKey, spaceName,
                    parentId, parentTitle, pageUrl(config, pageId));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La publicación en Confluence fue interrumpida.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("No fue posible publicar el documento en Confluence.", ex);
        }
    }

    private ExistingPage findExistingChild(IntegrationConfig config, String parentId, String title) throws Exception {
        String path = "/api/v2/pages/" + parentId + "/children?limit=" + PAGE_LIMIT;
        while (path != null) {
            HttpResponse<String> response = atlassian.confluenceGet(config, path);
            ensureSuccess(response, "validar documentos existentes");
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode page : results) {
                    if (title.equalsIgnoreCase(page.path("title").asText("")))
                        return new ExistingPage(page.path("id").asText(""));
                }
            }
            path = nextPath(root);
        }
        return null;
    }

    private int parentScore(String title, String documentType) {
        String normalized = normalize(title);
        if ("DPC".equalsIgnoreCase(documentType)) {
            if (normalized.equals("documento de pase a produccion dpc")) return 100;
            if (normalized.contains("documento de pase a produccion")) return 90;
            if (normalized.contains("pase a produccion")) return 75;
            if (normalized.matches(".*\\bdpc\\b.*")) return 60;
            return 0;
        }
        if (normalized.equals("definiciones tecnicas") || normalized.equals("definicion tecnica")) return 100;
        if (normalized.contains("definiciones tecnicas") || normalized.contains("definicion tecnica")) return 90;
        if (normalized.contains("definicion") && normalized.contains("tecnica")) return 75;
        return 0;
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
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

    private void ensureSuccess(HttpResponse<String> response, String action) {
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("Confluence respondió HTTP " + response.statusCode() + " al " + action + ".");
    }

    private void requireConfigured(IntegrationConfig config, String spaceId) {
        if (config == null || !config.complete()) throw new IllegalStateException("Confluence no está configurado.");
        if (blank(spaceId)) throw new IllegalArgumentException("Selecciona un Space de Confluence.");
    }

    private String pageUrl(IntegrationConfig config, String pageId) {
        String base = config.url().trim().replaceAll("/+$", "");
        int wiki = base.indexOf("/wiki");
        if (wiki >= 0) base = base.substring(0, wiki);
        return base + "/wiki/pages/viewpage.action?pageId=" + pageId;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private record ExistingPage(String id) { }
    public record ParentPage(String id, String title, int score) { }
    public record PublicationResult(boolean created, boolean alreadyExists, String pageId, String title,
                                    String spaceKey, String spaceName, String parentId, String parentTitle, String url) { }
}
