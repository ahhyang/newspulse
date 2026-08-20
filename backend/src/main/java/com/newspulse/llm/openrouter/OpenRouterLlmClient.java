package com.newspulse.llm.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newspulse.config.AppProperties;
import com.newspulse.domain.Sentiment;
import com.newspulse.llm.BatchSummaryParser;
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

	private static final String BATCH_SYSTEM_PROMPT = """
			You compose a combined AI-industry news briefing from several selected stories.
			Return ONLY a JSON object with these keys:
			- headline: one compelling line capturing the shared thread (max 120 chars)
			- overview: 3-5 sentences synthesizing what matters across the selection; mention tensions or trends
			- themes: array of 2-5 short theme labels (e.g. "chip supply", "regulation")
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
		String payload = complete(SYSTEM_PROMPT, buildUserPrompt(title, content));
		return parseEnrichment(payload);
	}

	@Override
	public BatchSummaryResult summarizeBatch(List<BatchArticle> articles) {
		if (llm.apiKey() == null || llm.apiKey().isBlank()) {
			throw new LlmException("OpenRouter API key is not configured");
		}
		if (articles == null || articles.size() < 2) {
			throw new LlmException("At least two articles are required for a batch summary");
		}
		String payload = complete(BATCH_SYSTEM_PROMPT, buildBatchPrompt(articles));
		return BatchSummaryParser.parse(payload, llm.model());
	}

	private String complete(String systemPrompt, String userPrompt) {
		OpenRouterChatResponse.ChatRequest request = new OpenRouterChatResponse.ChatRequest(
				llm.model(),
				List.of(
						new OpenRouterChatResponse.ChatMessage("system", systemPrompt),
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

	private EnrichmentResult parseEnrichment(String rawContent) {
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

	private String buildBatchPrompt(List<BatchArticle> articles) {
		StringBuilder builder = new StringBuilder("Selected stories (%d):%n%n".formatted(articles.size()));
		int index = 1;
		for (BatchArticle article : articles) {
			builder.append(index++)
					.append(". [")
					.append(article.sourceName())
					.append(" | ")
					.append(article.sentiment())
					.append("] ")
					.append(article.title())
					.append("\n")
					.append(article.snippet())
					.append("\n\n");
		}
		return builder.toString().strip();
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
