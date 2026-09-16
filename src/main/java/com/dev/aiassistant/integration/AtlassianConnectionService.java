package com.dev.aiassistant.integration;

import com.dev.aiassistant.config.model.IntegrationConfig;
import org.springframework.stereotype.Service;

import java.net.URI;
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
        if (!config.complete() || blank(config.context())) return ConnectionResult.error("Completa URL, espacio, usuario y token.");
        return test(config, "/rest/api/space/" + encodePath(config.context()), "Confluence", "Espacio " + config.context() + " accesible.");
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
            if (response.statusCode() == 401) return ConnectionResult.error(system + ": credenciales inválidas (401).");
            if (response.statusCode() == 403) return ConnectionResult.error(system + ": conexión realizada, pero el usuario no tiene permisos (403).");
            if (response.statusCode() == 404) return ConnectionResult.error(system + ": recurso configurado no encontrado (404).");
            return ConnectionResult.error(system + ": respuesta HTTP " + response.statusCode() + ".");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ConnectionResult.error(system + ": prueba interrumpida.");
        } catch (Exception ex) {
            return ConnectionResult.error(system + ": no fue posible establecer conexión. Revisa URL, red/VPN y configuración.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String encodePath(String value) { return value.trim().replace(" ", "%20"); }
}
