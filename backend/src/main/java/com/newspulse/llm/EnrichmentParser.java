package com.newspulse.llm;

import com.newspulse.domain.Sentiment;
import java.util.Locale;
import java.util.Set;

public final class EnrichmentParser {

	private static final Set<String> STANCE_TAGS = Set.of(
			"regulatory", "market", "technical", "research", "policy", "product", "legal", "other"
	);

	private EnrichmentParser() {}

	public static String extractJsonObject(String content) {
		if (content == null || content.isBlank()) {
			throw new LlmException("LLM returned an empty body");
		}
		String stripped = content.strip();
		if (stripped.startsWith("```")) {
			stripped = stripped.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").strip();
		}
		int start = stripped.indexOf('{');
		int end = stripped.lastIndexOf('}');
		if (start < 0 || end <= start) {
			throw new LlmException("LLM response did not contain a JSON object");
		}
		return stripped.substring(start, end + 1);
	}

	public static Sentiment parseSentiment(String raw) {
		if (raw == null || raw.isBlank()) {
			return Sentiment.NEUTRAL;
		}
		String normalized = raw.strip().toUpperCase(Locale.ROOT);
		if (normalized.startsWith("POS")) {
			return Sentiment.POSITIVE;
		}
		if (normalized.startsWith("NEG")) {
			return Sentiment.NEGATIVE;
		}
		return Sentiment.NEUTRAL;
	}

	public static String normalizeStance(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String normalized = raw.strip().toLowerCase(Locale.ROOT).replace(' ', '_');
		if (STANCE_TAGS.contains(normalized)) {
			return normalized;
		}
		return "other";
	}

	public static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		String stripped = value.strip();
		return stripped.length() <= max ? stripped : stripped.substring(0, max);
	}
}
