package com.newspulse.ingestion.gnews;

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
@ConditionalOnProperty(name = "app.gnews.enabled", havingValue = "true", matchIfMissing = true)
public class GnewsNewsSource implements NewsSource {

	private static final Logger log = LoggerFactory.getLogger(GnewsNewsSource.class);
	private static final String SOURCE_ID = "gnews";

	private final RestClient restClient;
	private final AppProperties.Gnews gnews;

	public GnewsNewsSource(RestClient.Builder restClientBuilder, AppProperties appProperties) {
		this.gnews = appProperties.gnews();
		this.restClient = restClientBuilder.clone().baseUrl(gnews.baseUrl()).build();
	}

	@Override
	public String id() {
		return SOURCE_ID;
	}

	@Override
	public List<RawArticle> fetch(Topic topic, Instant since) {
		if (gnews.apiKey() == null || gnews.apiKey().isBlank()) {
			log.warn("GNews API key is not configured; skipping fetch for topic '{}'", topic.getName());
			return List.of();
		}

		GnewsSearchResponse response = search(topic.getQuery(), since);
		if (response == null || response.articles() == null) {
			return List.of();
		}

		return response.articles().stream()
				.filter(article -> article.url() != null && !article.url().isBlank())
				.filter(article -> article.title() != null && !article.title().isBlank())
				.filter(article -> publishedAtOrMin(article).isAfter(since == null ? Instant.EPOCH : since.minusSeconds(1)))
				.map(this::toRawArticle)
				.toList();
	}

	private GnewsSearchResponse search(String query, Instant since) {
		try {
			return executeSearch(query, since);
		} catch (IngestionSourceException ex) {
			if (since != null) {
				log.warn("GNews search with 'from' failed ({}). Retrying without date filter.", ex.getMessage());
				return executeSearch(query, null);
			}
			throw ex;
		}
	}

	private GnewsSearchResponse executeSearch(String query, Instant since) {
		int attempt = 0;
		int maxAttempts = 3;
		long backoffMs = 400;
		while (true) {
			attempt++;
			try {
				Instant from = since;
				return restClient.get()
						.uri(uriBuilder -> {
							uriBuilder.path("/search")
									.queryParam("q", query)
									.queryParam("lang", gnews.lang())
									.queryParam("max", gnews.maxResults())
									.queryParam("sortby", "publishedAt")
									.queryParam("apikey", gnews.apiKey());
							if (from != null) {
								uriBuilder.queryParam("from", from.toString());
							}
							return uriBuilder.build();
						})
						.retrieve()
						.onStatus(HttpStatusCode::isError, (request, response) -> {
							String body = new String(response.getBody().readAllBytes());
							throw new IngestionSourceException(
									"GNews HTTP %s: %s".formatted(response.getStatusCode().value(), body.strip())
							);
						})
						.body(GnewsSearchResponse.class);
			} catch (IngestionSourceException | RestClientException ex) {
				boolean retryable = attempt < maxAttempts && isRetryable(ex);
				if (!retryable) {
					if (ex instanceof IngestionSourceException ingestionEx) {
						throw ingestionEx;
					}
					throw new IngestionSourceException("GNews request failed: " + ex.getMessage(), ex);
				}
				log.warn("GNews request attempt {}/{} failed: {}. Retrying in {}ms", attempt, maxAttempts, ex.getMessage(), backoffMs);
				sleep(backoffMs);
				backoffMs *= 2;
			}
		}
	}

	private RawArticle toRawArticle(GnewsSearchResponse.GnewsArticle article) {
		String sourceName = article.source() != null && article.source().name() != null
				? article.source().name()
				: SOURCE_ID;
		String content = firstNonBlank(article.content(), article.description(), article.title());
		return new RawArticle(
				article.title().strip(),
				article.url().strip(),
				SOURCE_ID,
				sourceName,
				publishedAtOrMin(article),
				content
		);
	}

	private static Instant publishedAtOrMin(GnewsSearchResponse.GnewsArticle article) {
		if (article.publishedAt() == null || article.publishedAt().isBlank()) {
			return Instant.EPOCH;
		}
		try {
			return Instant.parse(article.publishedAt());
		} catch (Exception ignored) {
			return OffsetDateTime.parse(article.publishedAt()).toInstant();
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
			throw new IngestionSourceException("Interrupted while backing off GNews retry", ex);
		}
	}
}
