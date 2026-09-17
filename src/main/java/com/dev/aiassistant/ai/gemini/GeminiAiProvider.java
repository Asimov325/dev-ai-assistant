package com.dev.aiassistant.ai.gemini;

import com.dev.aiassistant.ai.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    private final RestClient restClient;
    private final String configuredApiKey;
    private final String preferredModel;
    private volatile String runtimeApiKey;
    private volatile String runtimeModel;

    public GeminiAiProvider(RestClient.Builder builder,
                            @Value("${app.ai.gemini.api-key:}") String apiKey,
                            @Value("${app.ai.gemini.model:}") String model,
                            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.configuredApiKey = apiKey;
        this.preferredModel = model;
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public String modelId() {
        String model = runtimeModel;
        return model == null || model.isBlank() ? "no disponible" : model;
    }

    public void useRuntimeApiKey(String value) {
        runtimeApiKey = value;
        runtimeModel = null;
        log.info("Gemini: credencial de ejecución actualizada; se invalidó el modelo en memoria.");
    }

    public boolean configured() {
        return !apiKey().isBlank();
    }

    public String validateConnection() {
        String key = apiKey();
        if (key.isBlank()) throw new IllegalStateException("Ingresa una API key de Gemini.");
        long start = System.currentTimeMillis();
        log.info("Gemini: consultando modelos disponibles compatibles con generateContent.");
        try {
            ModelList response = restClient.get()
                    .uri(b -> b.path("/v1beta/models").queryParam("key", key).queryParam("pageSize", 1000).build())
                    .retrieve().body(ModelList.class);
            String selected = selectModel(response);
            runtimeModel = selected;
            log.info("Gemini: conexión validada. modelo={} tiempoMs={}", selected, System.currentTimeMillis() - start);
            return selected;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("Gemini: error validando conexión. httpStatus={} tiempoMs={}", status, System.currentTimeMillis() - start);
            if (status == 400 || status == 401 || status == 403)
                throw new IllegalStateException("Gemini no aceptó la API key o el proyecto no tiene acceso a Gemini (HTTP " + status + ").", ex);
            if (status == 429)
                throw new IllegalStateException("Gemini alcanzó el límite temporal/cuota de la API (HTTP 429).", ex);
            throw new IllegalStateException("No fue posible validar Gemini (HTTP " + status + ").", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException state) throw state;
            log.error("Gemini: error inesperado consultando modelos.", ex);
            throw new IllegalStateException("No fue posible comunicarse con Gemini para consultar los modelos disponibles.", ex);
        }
    }

    @Override
    public String generate(String prompt) {
        String key = apiKey();
        if (key.isBlank())
            throw new IllegalStateException("Gemini no está configurado. Ingresa y valida una API key en Configuración.");
        String selectedModel = runtimeModel;
        if (selectedModel == null || selectedModel.isBlank()) selectedModel = validateConnection();
        return generateWithModel(prompt, key, selectedModel, true);
    }

    private String generateWithModel(String prompt, String key, String selectedModel, boolean allowModelRefresh) {
        Map<String, Object> request = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        long start = System.currentTimeMillis();
        log.info("Gemini: iniciando generateContent. modelo={} promptChars={} reintentoDisponible={}",
                selectedModel, prompt == null ? 0 : prompt.length(), allowModelRefresh);
        try {
            GeminiResponse response = restClient.post()
                    .uri(b -> b.path("/v1beta/models/{model}:generateContent").queryParam("key", key).build(selectedModel))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(GeminiResponse.class);
            String text = extractText(response);
            runtimeModel = selectedModel;
            log.info("Gemini: generateContent completado. modelo={} respuestaChars={} tiempoMs={}",
                    selectedModel, text.length(), System.currentTimeMillis() - start);
            return text;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("Gemini: generateContent rechazado. modelo={} httpStatus={} tiempoMs={}",
                    selectedModel, status, System.currentTimeMillis() - start);
            if (status == 404 && allowModelRefresh) {
                runtimeModel = null;
                log.warn("Gemini: modelo {} no disponible; se consultará nuevamente el catálogo y se reintentará una sola vez.", selectedModel);
                String refreshedModel = validateConnection();
                if (refreshedModel.equals(selectedModel)) {
                    throw new IllegalStateException("Gemini informa que el modelo " + selectedModel + " soporta generateContent, pero la generación devuelve HTTP 404. Revisa la disponibilidad del modelo/proyecto.", ex);
                }
                return generateWithModel(prompt, key, refreshedModel, false);
            }
            if (status == 429)
                throw new IllegalStateException("Gemini alcanzó el límite temporal/cuota de la API (HTTP 429).", ex);
            if (status == 404)
                throw new IllegalStateException("El modelo de Gemini seleccionado no está disponible para generateContent después de actualizar el catálogo.", ex);
            throw new IllegalStateException("Gemini rechazó la generación (HTTP " + status + ").", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException state) throw state;
            log.error("Gemini: error inesperado durante generateContent. modelo={}", selectedModel, ex);
            throw new IllegalStateException("No fue posible comunicarse con Gemini.", ex);
        }
    }

    private String selectModel(ModelList response) {
        if (response == null || response.models() == null)
            throw new IllegalStateException("Gemini respondió sin modelos disponibles para esta API key.");
        List<ModelInfo> compatible = response.models().stream()
                .filter(m -> m.name() != null && m.supportedGenerationMethods() != null
                        && m.supportedGenerationMethods().contains("generateContent"))
                .toList();
        if (compatible.isEmpty())
            throw new IllegalStateException("La API key es válida, pero no tiene modelos disponibles que soporten generateContent.");

        if (preferredModel != null && !preferredModel.isBlank()) {
            String wanted = normalize(preferredModel);
            for (ModelInfo model : compatible) {
                if (normalize(model.name()).equals(wanted)) {
                    log.info("Gemini: se utilizará el modelo preferido configurado {}.", wanted);
                    return wanted;
                }
            }
            log.warn("Gemini: el modelo preferido {} no está disponible; se seleccionará uno del catálogo compatible.", wanted);
        }

        String selected = compatible.stream()
                .map(ModelInfo::name)
                .map(this::normalize)
                .sorted(Comparator.comparingInt(this::modelPreferenceScore).reversed().thenComparing(String::compareTo))
                .findFirst()
                .orElseThrow();
        log.info("Gemini: modelo seleccionado dinámicamente={} compatiblesEncontrados={}", selected, compatible.size());
        return selected;
    }

    private int modelPreferenceScore(String model) {
        String value = model.toLowerCase();
        int score = 0;
        if (value.contains("flash")) score += 100;
        if (value.contains("lite")) score += 30;
        if (value.contains("latest")) score += 20;
        if (value.contains("preview") || value.contains("experimental") || value.contains("exp")) score -= 80;
        if (value.contains("embedding") || value.contains("image") || value.contains("tts") || value.contains("audio")) score -= 200;
        return score;
    }

    private String normalize(String value) {
        return value.startsWith("models/") ? value.substring("models/".length()) : value;
    }

    private String apiKey() {
        String value = runtimeApiKey;
        return value != null && !value.isBlank() ? value : (configuredApiKey == null ? "" : configuredApiKey);
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty())
            throw new IllegalStateException("Gemini respondió sin contenido generado.");
        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty())
            throw new IllegalStateException("Gemini respondió sin texto generado.");
        String text = candidate.content().parts().stream().map(Part::text)
                .filter(v -> v != null && !v.isBlank()).reduce("", (a, b) -> a + b);
        if (text.isBlank()) throw new IllegalStateException("Gemini respondió sin texto generado.");
        return text;
    }

    public record ModelList(List<ModelInfo> models) { }
    public record ModelInfo(String name, List<String> supportedGenerationMethods) { }
    public record GeminiResponse(List<Candidate> candidates) { }
    public record Candidate(Content content) { }
    public record Content(List<Part> parts) { }
    public record Part(String text) { }
}
