package com.dev.aiassistant.ai.gemini;

import com.dev.aiassistant.ai.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GeminiAiProvider implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";

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
            List<String> compatible = compatibleModels(key);
            String selected = selectModel(compatible, Set.of(), true);
            runtimeModel = selected;
            log.info("Gemini: conexión validada. modelo={} compatiblesEncontrados={} tiempoMs={}",
                    selected, compatible.size(), System.currentTimeMillis() - start);
            return selected;
        } catch (RestClientResponseException ex) {
            throw connectionException(ex, start);
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

        List<String> compatible;
        try {
            compatible = compatibleModels(key);
        } catch (RestClientResponseException ex) {
            throw connectionException(ex, System.currentTimeMillis());
        }

        Set<String> rejected = new HashSet<>();
        String initial = runtimeModel;
        if (initial == null || initial.isBlank() || !compatible.contains(normalize(initial))) {
            initial = selectModel(compatible, rejected, true);
        }
        return generateWithFallback(prompt, key, compatible, initial, rejected);
    }

    private String generateWithFallback(String prompt, String key, List<String> compatible,
                                        String initialModel, Set<String> rejected) {
        String selectedModel = initialModel;
        RestClientResponseException last404 = null;

        while (selectedModel != null) {
            try {
                return generateOnce(prompt, key, selectedModel);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (status == 404) {
                    last404 = ex;
                    rejected.add(selectedModel);
                    runtimeModel = null;
                    log.warn("Gemini: modelo descartado para esta generación por HTTP 404. modelo={} descartados={}",
                            selectedModel, rejected.size());
                    selectedModel = selectModelOrNull(compatible, rejected, false);
                    if (selectedModel != null) {
                        log.info("Gemini: reintentando generateContent con modelo alternativo={}", selectedModel);
                    }
                    continue;
                }
                if (status == 429)
                    throw new IllegalStateException("Gemini alcanzó el límite temporal/cuota de la API (HTTP 429).", ex);
                if (status == 400 || status == 401 || status == 403)
                    throw new IllegalStateException("Gemini rechazó la generación o la API key/proyecto no tiene acceso (HTTP " + status + ").", ex);
                throw new IllegalStateException("Gemini rechazó la generación (HTTP " + status + ").", ex);
            }
        }

        throw new IllegalStateException(
                "Gemini no pudo generar contenido con ninguno de los modelos disponibles que anuncian soporte para generateContent.",
                last404);
    }

    private String generateOnce(String prompt, String key, String selectedModel) {
        Map<String, Object> request = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        long start = System.currentTimeMillis();
        log.info("Gemini: iniciando generateContent. modelo={} promptChars={}",
                selectedModel, prompt == null ? 0 : prompt.length());
        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", selectedModel)
                    .header(API_KEY_HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(GeminiResponse.class);
            String text = extractText(response);
            runtimeModel = selectedModel;
            log.info("Gemini: generateContent completado. modelo={} respuestaChars={} tiempoMs={}",
                    selectedModel, text.length(), System.currentTimeMillis() - start);
            return text;
        } catch (RestClientResponseException ex) {
            log.warn("Gemini: generateContent rechazado. modelo={} httpStatus={} tiempoMs={}",
                    selectedModel, ex.getStatusCode().value(), System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private List<String> compatibleModels(String key) {
        ModelList response = restClient.get()
                .uri(b -> b.path("/v1beta/models").queryParam("pageSize", 1000).build())
                .header(API_KEY_HEADER, key)
                .retrieve().body(ModelList.class);
        if (response == null || response.models() == null)
            throw new IllegalStateException("Gemini respondió sin modelos disponibles para esta API key.");

        List<String> compatible = response.models().stream()
                .filter(m -> m.name() != null && m.supportedGenerationMethods() != null
                        && m.supportedGenerationMethods().contains("generateContent"))
                .map(ModelInfo::name)
                .map(this::normalize)
                .distinct()
                .toList();
        if (compatible.isEmpty())
            throw new IllegalStateException("La API key es válida, pero no tiene modelos disponibles que soporten generateContent.");
        return compatible;
    }

    private String selectModel(List<String> compatible, Set<String> excluded, boolean allowPreferred) {
        String selected = selectModelOrNull(compatible, excluded, allowPreferred);
        if (selected == null)
            throw new IllegalStateException("No quedan modelos Gemini disponibles para generateContent después de descartar los que fallaron.");
        return selected;
    }

    private String selectModelOrNull(List<String> compatible, Set<String> excluded, boolean allowPreferred) {
        List<String> candidates = new ArrayList<>(compatible.stream()
                .filter(model -> !excluded.contains(model))
                .toList());
        if (candidates.isEmpty()) return null;

        if (allowPreferred && preferredModel != null && !preferredModel.isBlank()) {
            String wanted = normalize(preferredModel);
            if (candidates.contains(wanted)) {
                log.info("Gemini: se utilizará inicialmente el modelo preferido configurado {}.", wanted);
                return wanted;
            }
            log.warn("Gemini: el modelo preferido {} no está disponible; se seleccionará uno del catálogo compatible.", wanted);
        }

        String selected = candidates.stream()
                .sorted(Comparator.comparingInt(this::modelPreferenceScore).reversed().thenComparing(String::compareTo))
                .findFirst()
                .orElse(null);
        if (selected != null) {
            log.info("Gemini: modelo seleccionado dinámicamente={} candidatosDisponibles={} descartados={}",
                    selected, candidates.size(), excluded.size());
        }
        return selected;
    }

    private int modelPreferenceScore(String model) {
        String value = model.toLowerCase();
        int score = 0;
        if (value.matches(".*gemini-(3|4|5|6|7|8|9).*")) score += 200;
        if (value.contains("flash")) score += 100;
        if (value.contains("lite")) score += 30;
        if (value.contains("latest")) score += 20;
        if (value.contains("preview") || value.contains("experimental") || value.contains("exp")) score -= 80;
        if (value.contains("embedding") || value.contains("image") || value.contains("tts") || value.contains("audio")) score -= 200;
        return score;
    }

    private IllegalStateException connectionException(RestClientResponseException ex, long start) {
        int status = ex.getStatusCode().value();
        log.warn("Gemini: error validando conexión. httpStatus={} tiempoMs={}", status, Math.max(0, System.currentTimeMillis() - start));
        if (status == 400 || status == 401 || status == 403)
            return new IllegalStateException("Gemini no aceptó la API key o el proyecto no tiene acceso a Gemini (HTTP " + status + ").", ex);
        if (status == 429)
            return new IllegalStateException("Gemini alcanzó el límite temporal/cuota de la API (HTTP 429).", ex);
        return new IllegalStateException("No fue posible validar Gemini (HTTP " + status + ").", ex);
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
