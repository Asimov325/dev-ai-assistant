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
import java.util.Base64;

@Service
public class AtlassianConnectionService {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public ConnectionResult testJira(IntegrationConfig config) {
        if (!config.complete() || blank(config.context())) return ConnectionResult.error("Completa URL, proyecto, usuario y token.");
        return test(config, "/rest/api/3/project/" + encodePath(config.context()), "Jira", "Proyecto " + config.context() + " accesible.");
    }

    public ConnectionResult testConfluence(IntegrationConfig config) {
        if (!config.complete() || blank(config.context())) return ConnectionResult.error("Completa URL de Confluence, Space Key, usuario y token.");
        try {
            String baseUrl = normalizeConfluenceBaseUrl(config.url());
            String credentials = Base64.getEncoder().encodeToString((config.username() + ":" + config.secret()).getBytes(StandardCharsets.UTF_8));
            String key = URLEncoder.encode(config.context().trim(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/v2/spaces?keys=" + key + "&limit=10"))
                    .timeout(Duration.ofSeconds(15)).header("Accept", "application/json")
                    .header("Authorization", "Basic " + credentials).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body() == null ? "" : response.body();
                if (body.contains("\"key\":\"" + escapeJson(config.context().trim()) + "\"") || body.contains("\"key\": \"" + escapeJson(config.context().trim()) + "\""))
                    return ConnectionResult.ok("Conexión con Confluence correcta. Space Key " + config.context().trim() + " accesible.");
                return ConnectionResult.error("Confluence: conexión correcta, pero no se encontró el Space Key " + config.context().trim() + " con este usuario.");
            }
            return httpError("Confluence", response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ConnectionResult.error("Confluence: prueba interrumpida.");
        } catch (Exception ex) {
            return ConnectionResult.error("Confluence: no fue posible establecer conexión. Revisa URL, red/VPN y configuración.");
        }
    }

    private ConnectionResult test(IntegrationConfig config, String path, String system, String successDetail) {
        try {
            String baseUrl = config.url().replaceAll("/+$", "");
            String credentials = Base64.getEncoder().encodeToString((config.username() + ":" + config.secret()).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15)).header("Accept", "application/json")
                    .header("Authorization", "Basic " + credentials).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) return ConnectionResult.ok("Conexión con " + system + " correcta. " + successDetail);
            return httpError(system, response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ConnectionResult.error(system + ": prueba interrumpida.");
        } catch (Exception ex) {
            return ConnectionResult.error(system + ": no fue posible establecer conexión. Revisa URL, red/VPN y configuración.");
        }
    }

    private ConnectionResult httpError(String system, int status) {
        if (status == 401) return ConnectionResult.error(system + ": credenciales inválidas (401).");
        if (status == 403) return ConnectionResult.error(system + ": conexión realizada, pero el usuario no tiene permisos (403).");
        if (status == 404) return ConnectionResult.error(system + ": recurso configurado no encontrado (404).");
        return ConnectionResult.error(system + ": respuesta HTTP " + status + ".");
    }

    private String normalizeConfluenceBaseUrl(String value) {
        String url = value.trim().replaceAll("/+$", "");
        int wiki = url.indexOf("/wiki");
        if (wiki >= 0) return url.substring(0, wiki) + "/wiki";
        return url + "/wiki";
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String encodePath(String value) { return value.trim().replace(" ", "%20"); }
    private String escapeJson(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
