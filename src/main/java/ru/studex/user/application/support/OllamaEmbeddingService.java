package ru.studex.user.application.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Получает векторные эмбеддинги через локальный Ollama.
 * Модель: nomic-embed-text (768 измерений, многоязычная)
 * Запуск: ollama pull nomic-embed-text
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaEmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${support.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${support.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    /** Возвращает вектор эмбеддинга для переданного текста */
    public float[] embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Ollama /api/embed (новый API, Ollama 0.3+)
        Map<String, Object> body = Map.of("model", embeddingModel, "input", text);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EmbedResponse> response = restTemplate.exchange(
                    ollamaBaseUrl + "/api/embed",
                    HttpMethod.POST,
                    request,
                    EmbedResponse.class
            );
            if (response.getBody() != null && response.getBody().embeddings() != null
                    && !response.getBody().embeddings().isEmpty()) {
                return toFloatArray(response.getBody().embeddings().get(0));
            }
        } catch (Exception e) {
            log.warn("New Ollama embed API failed, trying legacy endpoint: {}", e.getMessage());
        }

        // Fallback: старый /api/embeddings (Ollama < 0.3)
        Map<String, Object> legacyBody = Map.of("model", embeddingModel, "prompt", text);
        HttpEntity<Map<String, Object>> legacyRequest = new HttpEntity<>(legacyBody, headers);
        ResponseEntity<LegacyEmbedResponse> legacyResp = restTemplate.exchange(
                ollamaBaseUrl + "/api/embeddings",
                HttpMethod.POST,
                legacyRequest,
                LegacyEmbedResponse.class
        );

        if (legacyResp.getBody() == null || legacyResp.getBody().embedding() == null) {
            throw new RuntimeException("Ollama returned empty embedding. Is the model downloaded? Run: ollama pull " + embeddingModel);
        }
        return toFloatArray(legacyResp.getBody().embedding());
    }

    private static float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            arr[i] = doubles.get(i).floatValue();
        }
        return arr;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbedResponse(List<List<Double>> embeddings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LegacyEmbedResponse(List<Double> embedding) {}
}
