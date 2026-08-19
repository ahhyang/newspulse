package com.newspulse.llm;

import com.newspulse.domain.Sentiment;

/**
 * LLM port. OpenRouter is the default adapter; swapping providers is a one-class change.
 */
public interface LlmClient {

	EnrichmentResult enrich(String title, String content);

	record EnrichmentResult(
			String summary,
			Sentiment sentiment,
			String justification,
			String stanceTag,
			String model
	) {}
}
