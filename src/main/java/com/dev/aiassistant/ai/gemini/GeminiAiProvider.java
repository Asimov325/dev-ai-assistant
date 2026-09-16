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
    private final RestClient restClient; private final String configuredApiKey; private final String model; private volatile String runtimeApiKey;
    public GeminiAiProvider(RestClient.Builder builder,@Value("${app.ai.gemini.api-key:}") String apiKey,@Value("${app.ai.gemini.model:gemini-2.5-flash-lite}") String model,@Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl){this.restClient=builder.baseUrl(baseUrl).build();this.configuredApiKey=apiKey;this.model=model;}
    @Override public String id(){return "gemini";}
    public void useRuntimeApiKey(String value){runtimeApiKey=value;}
    public boolean configured(){return !apiKey().isBlank();}
    @Override public String generate(String prompt){String key=apiKey();if(key.isBlank())throw new IllegalStateException("Gemini no está configurado. Ingresa y valida una API key en Configuración.");Map<String,Object> request=Map.of("contents",List.of(Map.of("parts",List.of(Map.of("text",prompt)))));try{GeminiResponse response=restClient.post().uri(b->b.path("/v1beta/models/{model}:generateContent").queryParam("key",key).build(model)).contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(GeminiResponse.class);return extractText(response);}catch(RestClientResponseException ex){throw new IllegalStateException("Gemini rechazó la solicitud (HTTP "+ex.getStatusCode().value()+"). Revisa la API key, el modelo y los límites del servicio gratuito.",ex);}catch(RuntimeException ex){if(ex instanceof IllegalStateException state)throw state;throw new IllegalStateException("No fue posible comunicarse con Gemini.",ex);}}
    private String apiKey(){String value=runtimeApiKey;return value!=null&&!value.isBlank()?value:(configuredApiKey==null?"":configuredApiKey);}
    private String extractText(GeminiResponse response){if(response==null||response.candidates()==null||response.candidates().isEmpty())throw new IllegalStateException("Gemini respondió sin contenido generado.");Candidate candidate=response.candidates().getFirst();if(candidate.content()==null||candidate.content().parts()==null||candidate.content().parts().isEmpty())throw new IllegalStateException("Gemini respondió sin texto generado.");String text=candidate.content().parts().stream().map(Part::text).filter(v->v!=null&&!v.isBlank()).reduce("",(a,b)->a+b);if(text.isBlank())throw new IllegalStateException("Gemini respondió sin texto generado.");return text;}
    public record GeminiResponse(List<Candidate> candidates){} public record Candidate(Content content){} public record Content(List<Part> parts){} public record Part(String text){}
}
