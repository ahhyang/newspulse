package com.newspulse.llm.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newspulse.config.AppProperties;
import com.newspulse.domain.Sentiment;
import com.newspulse.llm.EnrichmentParser;
import com.newspulse.llm.LlmClient;
import com.newspulse.llm.LlmException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "app.llm.enabled", havingValue = "true", matchIfMissing = true)
public class OpenRouterLlmClient implements LlmClient {

	private static final Logger log = LoggerFactory.getLogger(OpenRouterLlmClient.class);

	private static final String SYSTEM_PROMPT = """
			You enrich news articles for an AI-industry briefing product.
			Return ONLY a JSON object with these keys:
			- summary: 2-3 sentences, factual, no hype
			- sentiment: POSITIVE, NEUTRAL, or NEGATIVE (coverage tone toward the subject)
			- justification: one short sentence explaining the sentiment
			- stance: one of regulatory, market, technical, research, policy, product, legal, other
			No markdown. No extra keys.
			""";

	private final RestClient restClient;
	private final AppProperties.Llm llm;
	private final ObjectMapper objectMapper;

	public OpenRouterLlmClient(RestClient.Builder restClientBuilder, AppProperties appProperties, ObjectMapper objectMapper) {
		this.llm = appProperties.llm();
		this.objectMapper = objectMapper;
		this.restClient = restClientBuilder.clone().baseUrl(llm.baseUrl()).build();
	}

	@Override
	public EnrichmentResult enrich(String title, String content) {
		if (llm.apiKey() == null || llm.apiKey().isBlank()) {
			throw new LlmException("OpenRouter API key is not configured");
		}
		String payload = complete(buildUserPrompt(title, content));
		return parse(payload);
	}

	private String complete(String userPrompt) {
		OpenRouterChatResponse.ChatRequest request = new OpenRouterChatResponse.ChatRequest(
				llm.model(),
				List.of(
						new OpenRouterChatResponse.ChatMessage("system", SYSTEM_PROMPT),
						new OpenRouterChatResponse.ChatMessage("user", userPrompt)
				),
				0.2,
				llm.maxTokens()
		);
		int attempt = 0;
		int maxAttempts = 3;
		long backoffMs = 400;
		while (true) {
			attempt++;
			try {
				OpenRouterChatResponse response = restClient.post()
						.uri("/chat/completions")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + llm.apiKey())
						.header("HTTP-Referer", llm.httpReferer())
						.header("X-Title", llm.appTitle())
						.body(request)
						.retrieve()
						.onStatus(HttpStatusCode::isError, (req, res) -> {
							String body = new String(res.getBody().readAllBytes());
							throw new LlmException("OpenRouter HTTP %s: %s".formatted(res.getStatusCode().value(), body.strip()));
						})
						.body(OpenRouterChatResponse.class);
				if (response == null || response.choices() == null || response.choices().isEmpty()
						|| response.choices().getFirst().message() == null) {
					throw new LlmException("OpenRouter returned no choices");
				}
				return response.choices().getFirst().message().content();
			} catch (LlmException | RestClientException ex) {
				boolean retryable = attempt < maxAttempts && isRetryable(ex);
				if (!retryable) {
					if (ex instanceof LlmException llmEx) {
						throw llmEx;
					}
					throw new LlmException("OpenRouter request failed: " + ex.getMessage(), ex);
				}
				log.warn("OpenRouter attempt {}/{} failed: {}. Retrying in {}ms", attempt, maxAttempts, ex.getMessage(), backoffMs);
				sleep(backoffMs);
				backoffMs *= 2;
			}
		}
	}

	private EnrichmentResult parse(String rawContent) {
		try {
			JsonNode node = objectMapper.readTree(EnrichmentParser.extractJsonObject(rawContent));
			String summary = text(node, "summary");
			if (summary.isBlank()) {
				throw new LlmException("LLM JSON was missing summary");
			}
			Sentiment sentiment = EnrichmentParser.parseSentiment(text(node, "sentiment"));
			String justification = EnrichmentParser.truncate(text(node, "justification"), 500);
			if (justification.isBlank()) {
				justification = "No justification provided";
			}
			String stance = EnrichmentParser.normalizeStance(text(node, "stance"));
			return new EnrichmentResult(summary, sentiment, justification, stance, llm.model());
		} catch (LlmException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new LlmException("Failed to parse LLM JSON: " + ex.getMessage(), ex);
		}
	}

	private String buildUserPrompt(String title, String content) {
		String body = content == null ? "" : content.strip();
		int max = Math.max(500, llm.maxContentChars());
		if (body.length() > max) {
			body = body.substring(0, max);
		}
		return "Title: %s%n%nArticle:%n%s".formatted(title == null ? "" : title.strip(), body);
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? "" : value.asText("");
	}

	private static boolean isRetryable(Exception ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
		return message.contains("http 429")
				|| message.contains("http 500")
				|| message.contains("http 502")
				|| message.contains("http 503")
				|| message.contains("http 504")
				|| message.contains("timeout")
				|| message.contains("temporarily");
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new LlmException("Interrupted while backing off OpenRouter retry", ex);
		}
	}
}
