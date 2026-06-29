package ru.studex.user.application.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqChatService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;

    @Value("${support.groq.api-key}")
    private String apiKey;

    @Value("${support.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${support.groq.max-tokens:500}")
    private int maxTokens;

    @Value("${support.groq.temperature:0.3}")
    private double temperature;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY is not set! Support AI will not work. " +
                     "Set environment variable GROQ_API_KEY or add support.groq.api-key to application.yml");
        } else {
            log.info("Groq API configured: model={}, key={}...{}",
                    model,
                    apiKey.substring(0, Math.min(8, apiKey.length())),
                    apiKey.substring(Math.max(0, apiKey.length() - 4)));
        }
    }

    public String chat(List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Groq API key not configured");
            return "AI-ответы не настроены. Обратитесь к администратору или создайте тикет.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "max_tokens", maxTokens,
                "temperature", temperature
        );

        try {
            log.debug("Calling Groq API: model={}, messages={}", model, messages.size());

            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    GROQ_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    ChatResponse.class
            );

            if (response.getBody() != null
                    && response.getBody().choices() != null
                    && !response.getBody().choices().isEmpty()) {
                String content = response.getBody().choices().get(0).message().content();
                log.debug("Groq responded: {} chars", content.length());
                return content;
            }

            log.warn("Groq returned empty choices. Body: {}", response.getBody());
            return "Не удалось получить ответ от AI. Попробуйте ещё раз.";

        } catch (HttpClientErrorException e) {
            // 4xx — обычно неверный ключ (401) или квота (429)
            log.error("Groq HTTP {} error: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "Неверный API-ключ Groq. Проверьте переменную GROQ_API_KEY.";
            }
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "Превышен лимит запросов к AI. Попробуйте через минуту.";
            }
            return "Ошибка AI (" + e.getStatusCode() + "). Создайте тикет для помощи оператора.";

        } catch (HttpServerErrorException e) {
            // 5xx — проблемы на стороне Groq
            log.error("Groq server error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Сервис AI временно недоступен. Попробуйте через несколько минут.";

        } catch (Exception e) {
            log.error("Groq unexpected error: {}", e.getMessage(), e);
            return "Не удалось соединиться с AI. Проверьте интернет-соединение или создайте тикет.";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String role, String content) {}
}
