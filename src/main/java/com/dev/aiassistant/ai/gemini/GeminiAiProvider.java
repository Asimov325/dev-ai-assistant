package com.dev.aiassistant.ai.gemini;

import com.dev.aiassistant.ai.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiAiProvider(RestClient.Builder restClientBuilder,
                            @Value("${app.ai.gemini.api-key:}") String apiKey,
                            @Value("${app.ai.gemini.model:gemini-2.5-flash-lite}") String model,
                            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini no está configurado. Define la variable de entorno GEMINI_API_KEY.");
        }

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractText(response);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Gemini rechazó la solicitud (HTTP " + exception.getStatusCode().value() + "). Revisa la API key, el modelo y los límites del servicio gratuito.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException("No fue posible comunicarse con Gemini.", exception);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini respondió sin contenido generado.");
        }

        Candidate candidate = response.candidates().getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini respondió sin texto generado.");
        }

        String text = candidate.content().parts().stream()
                .map(Part::text)
                .filter(value -> value != null && !value.isBlank())
                .reduce("", (left, right) -> left + right);

        if (text.isBlank()) {
            throw new IllegalStateException("Gemini respondió sin texto generado.");
        }
        return text;
    }

    public record GeminiResponse(List<Candidate> candidates) {
    }

    public record Candidate(Content content) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
