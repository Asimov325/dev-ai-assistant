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
    private static final int MAX_TRANSIENT_RETRIES_PER_MODEL = 1;
    private static final long TRANSIENT_RETRY_DELAY_MS = 1500L;

    private final RestClient restClient;
    private final String configuredApiKey;
    private volatile String runtimeApiKey;
    private volatile String runtimeModel;
    private final Set<String> unavailableModels = new HashSet<>();

    public GeminiAiProvider(RestClient.Builder builder,
                            @Value("${app.ai.gemini.api-key:}") String apiKey,
                            @Value("${app.ai.gemini.model:}") String ignoredLegacyModel,
                            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.configuredApiKey = apiKey;
    }

    @Override
    public String id() { return "gemini"; }

    @Override
    public String modelId() {
        String model = runtimeModel;
        return model == null || model.isBlank() ? "no disponible" : model;
    }

    public synchronized void useRuntimeApiKey(String value) {
        runtimeApiKey = value;
        runtimeModel = null;
        unavailableModels.clear();
        log.info("Gemini: credencial de ejecución actualizada; se invalidó la selección dinámica de modelos.");
    }

    public boolean configured() { return !apiKey().isBlank(); }

    public String validateConnection() {
        String key = apiKey();
        if (key.isBlank()) throw new IllegalStateException("Ingresa una API key de Gemini.");
        long start = System.currentTimeMillis();
        log.info("Gemini: consultando catálogo disponible para seleccionar dinámicamente un modelo de texto con generateContent.");
        try {
            List<String> compatible = compatibleModels(key);
            String selected = selectModel(compatible, unavailableSnapshot());
            runtimeModel = selected;
            log.info("Gemini: conexión validada. candidatoDinamico={} compatiblesEncontrados={} descartadosSesion={} tiempoMs={}",
                    selected, compatible.size(), unavailableSnapshot().size(), System.currentTimeMillis() - start);
            return selected;
        } catch (RestClientResponseException ex) {
            throw connectionException(ex, start);
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

        Set<String> rejectedThisRequest = unavailableSnapshot();
        String selectedModel = selectModel(compatible, rejectedThisRequest);
        RestClientResponseException lastTransient = null;

        while (selectedModel != null) {
            for (int attempt = 1; attempt <= MAX_TRANSIENT_RETRIES_PER_MODEL + 1; attempt++) {
                try {
                    log.info("Gemini: generación. modelo={} intento={}/{} candidatosRestantes={}",
                            selectedModel, attempt, MAX_TRANSIENT_RETRIES_PER_MODEL + 1,
                            compatible.stream().filter(m -> !rejectedThisRequest.contains(m)).count());
                    return generateOnce(prompt, key, selectedModel);
                } catch (RestClientResponseException ex) {
                    int status = ex.getStatusCode().value();
                    if (status == 404) {
                        rejectModel(selectedModel, rejectedThisRequest, "HTTP 404");
                        break;
                    }
                    if (status == 503 || status == 429) {
                        lastTransient = ex;
                        if (attempt <= MAX_TRANSIENT_RETRIES_PER_MODEL) {
                            log.warn("Gemini: error transitorio HTTP {}. modelo={} intento={}; se reintentará una vez tras {} ms.",
                                    status, selectedModel, attempt, TRANSIENT_RETRY_DELAY_MS);
                            sleepBeforeRetry();
                            continue;
                        }
                        rejectModel(selectedModel, rejectedThisRequest, "HTTP " + status + " persistente");
                        break;
                    }
                    if (status == 400 || status == 401 || status == 403)
                        throw new IllegalStateException("Gemini rechazó la generación o la API key/proyecto no tiene acceso (HTTP " + status + ").", ex);
                    throw new IllegalStateException("Gemini rechazó la generación (HTTP " + status + ").", ex);
                }
            }
            selectedModel = selectModelOrNull(compatible, rejectedThisRequest);
            if (selectedModel != null)
                log.info("Gemini: continuando con modelo alternativo dinámico={}", selectedModel);
        }

        throw new IllegalStateException(
                "Gemini no pudo generar el documento con los modelos de texto disponibles. Los modelos que devolvieron 404/429/503 fueron descartados automáticamente para evitar repetir el mismo fallo.",
                lastTransient);
    }

    private String generateOnce(String prompt, String key, String selectedModel) {
        Map<String, Object> request = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        long start = System.currentTimeMillis();
        log.info("Gemini: iniciando generateContent. modelo={} promptChars={}", selectedModel, prompt == null ? 0 : prompt.length());
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
                .map(ModelInfo::name).map(this::normalize).distinct()
                .filter(this::isTextGenerationCandidate)
                .toList();
        if (compatible.isEmpty())
            throw new IllegalStateException("La API key es válida, pero no tiene modelos de texto disponibles que soporten generateContent.");
        return compatible;
    }

    private boolean isTextGenerationCandidate(String model) {
        String value = model.toLowerCase();
        if (!value.startsWith("gemini-")) return false;
        return !(value.contains("embedding") || value.contains("image") || value.contains("tts")
                || value.contains("audio") || value.contains("live") || value.contains("transcribe")
                || value.contains("robotics"));
    }

    private String selectModel(List<String> compatible, Set<String> excluded) {
        String selected = selectModelOrNull(compatible, excluded);
        if (selected == null)
            throw new IllegalStateException("No quedan modelos Gemini de texto disponibles para generateContent después de descartar los que fallaron.");
        return selected;
    }

    private String selectModelOrNull(List<String> compatible, Set<String> excluded) {
        List<String> candidates = new ArrayList<>(compatible.stream().filter(model -> !excluded.contains(model)).toList());
        if (candidates.isEmpty()) return null;
        String selected = candidates.stream()
                .sorted(Comparator.comparingInt(this::modelPreferenceScore).reversed().thenComparing(String::compareTo))
                .findFirst().orElse(null);
        if (selected != null)
            log.info("Gemini: modelo seleccionado dinámicamente={} candidatosTextoDisponibles={} descartados={}",
                    selected, candidates.size(), excluded.size());
        return selected;
    }

    private int modelPreferenceScore(String model) {
        String value = model.toLowerCase();
        int score = 0;
        // La selección se basa en el catálogo que devuelve la API; no se fija una versión concreta.
        if (value.contains("flash")) score += 100;
        if (value.contains("lite")) score += 20;
        if (value.contains("latest")) score += 10;
        if (value.contains("preview") || value.contains("experimental") || value.contains("exp")) score -= 80;
        if (value.contains("pro")) score -= 10;
        return score;
    }

    private synchronized void rejectModel(String model, Set<String> requestRejected, String reason) {
        requestRejected.add(model);
        unavailableModels.add(model);
        if (model.equals(runtimeModel)) runtimeModel = null;
        log.warn("Gemini: modelo descartado temporalmente. modelo={} motivo={} descartadosSolicitud={} descartadosSesion={}",
                model, reason, requestRejected.size(), unavailableModels.size());
    }

    private synchronized Set<String> unavailableSnapshot() { return new HashSet<>(unavailableModels); }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(TRANSIENT_RETRY_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La generación con Gemini fue interrumpida durante el reintento.", ex);
        }
    }

    private IllegalStateException connectionException(RestClientResponseException ex, long start) {
        int status = ex.getStatusCode().value();
        log.warn("Gemini: error validando conexión. httpStatus={} tiempoMs={}", status, Math.max(0, System.currentTimeMillis() - start));
        if (status == 400 || status == 401 || status == 403)
            return new IllegalStateException("Gemini no aceptó la API key o el proyecto no tiene acceso a Gemini (HTTP " + status + ").", ex);
        if (status == 429)
            return new IllegalStateException("Gemini alcanzó el límite temporal/cuota de la API (HTTP 429).", ex);
        if (status == 503)
            return new IllegalStateException("Gemini está temporalmente no disponible (HTTP 503).", ex);
        return new IllegalStateException("No fue posible validar Gemini (HTTP " + status + ").", ex);
    }

    private String normalize(String value) { return value.startsWith("models/") ? value.substring("models/".length()) : value; }

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
