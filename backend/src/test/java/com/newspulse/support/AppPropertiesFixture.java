package com.newspulse.support;

import com.newspulse.config.AppProperties;

public final class AppPropertiesFixture {

	private AppPropertiesFixture() {}

	public static AppProperties defaults() {
		return defaults("test");
	}

	public static AppProperties defaults(String llmApiKey) {
		return new AppProperties(
				new AppProperties.Jwt("test-jwt-secret-key-that-is-long-enough-for-hs256-algorithms", 3600000),
				new AppProperties.Admin("admin", "s3cret"),
				new AppProperties.Cors("http://localhost:5173"),
				new AppProperties.Ingestion(false, 3600000, 20000, 24),
				new AppProperties.Enrichment(true, 120000, 40000, 8),
				new AppProperties.Llm(
						true,
						"openrouter",
						"https://openrouter.ai/api/v1",
						llmApiKey,
						"anthropic/claude-3.5-sonnet",
						"https://github.com/ahhyang/newspulse",
						"NewsPulse",
						500,
						4000
				),
				new AppProperties.Gnews(true, "gnews-key", "https://gnews.io/api/v4", "en", 10)
		);
	}
}
