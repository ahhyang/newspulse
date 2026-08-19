package com.newspulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		Jwt jwt,
		Admin admin,
		Cors cors,
		Ingestion ingestion,
		Llm llm,
		Gnews gnews
) {
	public record Jwt(String secret, long expirationMs) {}

	public record Admin(String username, String password) {}

	public record Cors(String allowedOrigins) {}

	public record Ingestion(long intervalMs) {}

	public record Llm(
			String provider,
			String baseUrl,
			String apiKey,
			String model,
			String httpReferer,
			String appTitle
	) {}

	public record Gnews(String apiKey, String baseUrl) {}
}
