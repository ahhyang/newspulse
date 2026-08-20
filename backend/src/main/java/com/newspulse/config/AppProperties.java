package com.newspulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		Jwt jwt,
		Admin admin,
		Cors cors,
		Ingestion ingestion,
		Enrichment enrichment,
		Digest digest,
		Llm llm,
		Gnews gnews,
		HackerNews hackernews
) {
	public record Jwt(String secret, long expirationMs) {}

	public record Admin(String username, String password) {}

	public record Cors(String allowedOrigins) {}

	public record Ingestion(boolean enabled, long intervalMs, long initialDelayMs, int lookbackHours) {}

	public record Enrichment(boolean enabled, long intervalMs, long initialDelayMs, int batchSize) {}

	public record Digest(boolean enabled, String cron, int maxItems, double clusterSimilarity) {}

	public record Llm(
			boolean enabled,
			String provider,
			String baseUrl,
			String apiKey,
			String model,
			String httpReferer,
			String appTitle,
			int maxTokens,
			int maxContentChars
	) {}

	public record Gnews(
			boolean enabled,
			String apiKey,
			String baseUrl,
			String lang,
			int maxResults
	) {}

	public record HackerNews(
			boolean enabled,
			String baseUrl,
			int maxResults
	) {}
}
