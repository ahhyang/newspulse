package com.newspulse.ingestion.hackernews;

import com.newspulse.config.AppProperties;
import com.newspulse.domain.Topic;
import com.newspulse.ingestion.IngestionSourceException;
import com.newspulse.ingestion.NewsSource;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "app.hackernews.enabled", havingValue = "true", matchIfMissing = true)
public class HackerNewsSource implements NewsSource {

	private static final Logger log = LoggerFactory.getLogger(HackerNewsSource.class);
	private static final String SOURCE_ID = "hackernews";
	private static final String SOURCE_NAME = "Hacker News";
	private static final String ITEM_URL = "https://news.ycombinator.com/item?id=";

	private final RestClient restClient;
	private final AppProperties.HackerNews hackerNews;

	public HackerNewsSource(RestClient.Builder restClientBuilder, AppProperties appProperties) {
		this.hackerNews = appProperties.hackernews();
		this.restClient = restClientBuilder.clone().baseUrl(hackerNews.baseUrl()).build();
	}

	@Override
	public String id() {
		return SOURCE_ID;
	}

	@Override
	public List<RawArticle> fetch(Topic topic, Instant since) {
		HackerNewsSearchResponse response = executeSearch(topic.getQuery(), since);
		if (response == null || response.hits() == null) {
			return List.of();
		}

		Instant cutoff = since == null ? Instant.EPOCH : since.minusSeconds(1);
		return response.hits().stream()
				.filter(hit -> hit.title() != null && !hit.title().isBlank())
				.map(this::toRawArticle)
				.filter(article -> article.url() != null && !article.url().isBlank())
				.filter(article -> article.publishedAt().isAfter(cutoff))
				.toList();
	}

	private HackerNewsSearchResponse executeSearch(String query, Instant since) {
		int attempt = 0;
		int maxAttempts = 3;
		long backoffMs = 400;
		while (true) {
			attempt++;
			try {
				Instant from = since;
				return restClient.get()
						.uri(uriBuilder -> {
							uriBuilder.path("/search_by_date")
									.queryParam("query", query)
									.queryParam("tags", "story")
									.queryParam("hitsPerPage", hackerNews.maxResults());
							if (from != null) {
								uriBuilder.queryParam("numericFilters", "created_at_i>" + from.getEpochSecond());
							}
							return uriBuilder.build();
						})
						.retrieve()
						.onStatus(HttpStatusCode::isError, (request, response) -> {
							String body = new String(response.getBody().readAllBytes());
							throw new IngestionSourceException(
									"Hacker News HTTP %s: %s".formatted(response.getStatusCode().value(), body.strip())
							);
						})
						.body(HackerNewsSearchResponse.class);
			} catch (IngestionSourceException | RestClientException ex) {
				boolean retryable = attempt < maxAttempts && isRetryable(ex);
				if (!retryable) {
					if (ex instanceof IngestionSourceException ingestionEx) {
						throw ingestionEx;
					}
					throw new IngestionSourceException("Hacker News request failed: " + ex.getMessage(), ex);
				}
				log.warn(
						"Hacker News request attempt {}/{} failed: {}. Retrying in {}ms",
						attempt,
						maxAttempts,
						ex.getMessage(),
						backoffMs
				);
				sleep(backoffMs);
				backoffMs *= 2;
			}
		}
	}

	private RawArticle toRawArticle(HackerNewsSearchResponse.Hit hit) {
		String url = firstNonBlank(hit.url(), hnItemUrl(hit.objectID()));
		String content = firstNonBlank(hit.storyText(), hit.title());
		return new RawArticle(
				hit.title().strip(),
				url,
				SOURCE_ID,
				SOURCE_NAME,
				publishedAtOrMin(hit),
				content
		);
	}

	private static String hnItemUrl(String objectId) {
		if (objectId == null || objectId.isBlank()) {
			return "";
		}
		return ITEM_URL + objectId.strip();
	}

	private static Instant publishedAtOrMin(HackerNewsSearchResponse.Hit hit) {
		if (hit.createdAt() == null || hit.createdAt().isBlank()) {
			return Instant.EPOCH;
		}
		try {
			return Instant.parse(hit.createdAt());
		} catch (Exception ignored) {
			return OffsetDateTime.parse(hit.createdAt()).toInstant();
		}
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.strip();
			}
		}
		return "";
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
			throw new IngestionSourceException("Interrupted while backing off Hacker News retry", ex);
		}
	}
}
