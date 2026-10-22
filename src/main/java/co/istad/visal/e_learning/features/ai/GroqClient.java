package co.istad.visal.e_learning.features.ai;

import co.istad.visal.e_learning.expection.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chat completions through Groq (OpenAI-compatible API). Used by the lesson tutor and the quiz AI helper.
 * The key is read from configuration (groq.api-key, normally the GROQ_API_KEY environment variable or the
 * git-ignored secrets.properties) and never leaves the backend.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    public record Message(String role, String content) {
        public static Message user(String content) { return new Message("user", content); }
        public static Message assistant(String content) { return new Message("assistant", content); }
    }

    private final RestClient http;
    private final String model;
    private final String reasoningEffort; // for reasoning models such as openai/gpt-oss-*; blank = not sent
    private final boolean enabled;

    public GroqClient(@Value("${groq.api-key:}") String apiKey,
                      @Value("${groq.model:openai/gpt-oss-120b}") String model,
                      @Value("${groq.reasoning-effort:low}") String reasoningEffort,
                      @Value("${groq.base-url:https://api.groq.com/openai/v1}") String baseUrl) {
        this.model = model;
        this.reasoningEffort = reasoningEffort == null ? "" : reasoningEffort.trim();
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + (enabled ? apiKey.trim() : ""))
                .build();
        if (!enabled) log.warn("AI features disabled: set GROQ_API_KEY (or groq.api-key) to enable them");
    }

    public boolean isEnabled() {
        return enabled;
    }

    private Map<String, Object> body(List<Map<String, String>> messages, int maxTokens, double temperature, String reasoningEffort) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        // reasoning models spend part of the budget thinking, so leave headroom for the answer
        body.put("max_tokens", reasoningEffort.isEmpty() ? maxTokens : maxTokens + 1024);
        body.put("temperature", temperature);
        if (!reasoningEffort.isEmpty()) body.put("reasoning_effort", reasoningEffort);
        return body;
    }

    /** One completion. System prompt first, then the conversation (oldest first). */
    @SuppressWarnings("unchecked")
    public String chat(String system, List<Message> messages, int maxTokens, double temperature) {
        return chat(system, messages, maxTokens, temperature, reasoningEffort);
    }

    /** Same, with a reasoning effort for this call ("low" | "medium" | "high"; blank = not sent). */
    public String chat(String system, List<Message> messages, int maxTokens, double temperature, String effort) {
        if (!enabled) {
            throw new ServiceUnavailableException("The AI assistant isn't set up yet. Ask an admin to add an API key.");
        }
        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content", system));
        messages.forEach(m -> payload.add(Map.of("role", m.role(), "content", m.content())));

        try {
            Map<String, Object> response = http.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(payload, maxTokens, temperature, effort == null ? "" : effort.trim()))
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> choices = response == null ? List.of() : (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "…";
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message == null ? null : message.get("content");
            String text = content == null ? "" : content.toString().trim();
            return text.isEmpty() ? "…" : text;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new ServiceUnavailableException("The AI assistant is busy right now. Try again in a minute.");
            }
            log.warn("Groq call failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceUnavailableException("The AI assistant couldn't answer right now. Try again shortly.");
        } catch (RuntimeException e) {
            if (e instanceof ServiceUnavailableException) throw e;
            log.warn("Groq call failed: {}", e.getMessage());
            throw new ServiceUnavailableException("The AI assistant couldn't answer right now. Try again shortly.");
        }
    }
}
