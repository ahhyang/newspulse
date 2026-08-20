package com.newspulse.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public final class BatchSummaryParser {

	private BatchSummaryParser() {}

	public static LlmClient.BatchSummaryResult parse(String rawContent, String model) {
		try {
			JsonNode node = new ObjectMapper().readTree(EnrichmentParser.extractJsonObject(rawContent));
			String headline = text(node, "headline");
			String overview = text(node, "overview");
			if (headline.isBlank() || overview.isBlank()) {
				throw new LlmException("LLM JSON was missing headline or overview");
			}
			List<String> themes = new ArrayList<>();
			JsonNode themesNode = node.get("themes");
			if (themesNode != null && themesNode.isArray()) {
				themesNode.forEach(item -> {
					if (item.isTextual() && !item.asText("").isBlank()) {
						themes.add(item.asText("").strip());
					}
				});
			}
			return new LlmClient.BatchSummaryResult(headline.strip(), overview.strip(), List.copyOf(themes), model);
		} catch (LlmException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new LlmException("Failed to parse batch summary JSON: " + ex.getMessage(), ex);
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value == null || value.isNull() ? "" : value.asText("");
	}
}
