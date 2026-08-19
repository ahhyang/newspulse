package com.newspulse.llm.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterChatResponse(
		String model,
		List<Choice> choices
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Choice(Message message) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Message(String role, String content) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatRequest(
			String model,
			List<ChatMessage> messages,
			double temperature,
			@JsonProperty("max_tokens") int maxTokens
	) {}

	public record ChatMessage(String role, String content) {}
}
