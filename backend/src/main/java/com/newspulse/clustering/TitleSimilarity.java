package com.newspulse.clustering;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cheap, deterministic near-duplicate signal for headlines.
 * Embedding/LLM comparison can sit behind the same clustering service later.
 */
public final class TitleSimilarity {

	private static final Set<String> STOPWORDS = Set.of(
			"a", "an", "the", "and", "or", "of", "in", "on", "for", "to", "from", "by", "as", "at",
			"with", "after", "over", "into", "about", "new", "says", "say", "report", "reports",
			"is", "are", "be", "its", "it", "this", "that"
	);

	private TitleSimilarity() {}

	public static double jaccard(String left, String right) {
		Set<String> a = tokens(left);
		Set<String> b = tokens(right);
		if (a.isEmpty() || b.isEmpty()) {
			return 0;
		}
		long intersection = a.stream().filter(b::contains).count();
		int union = a.size() + b.size() - (int) intersection;
		return union == 0 ? 0 : (double) intersection / union;
	}

	public static Set<String> tokens(String title) {
		if (title == null || title.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(title.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
				.map(TitleSimilarity::stemLite)
				.filter(token -> token.length() > 2)
				.filter(token -> !STOPWORDS.contains(token))
				.collect(Collectors.toSet());
	}

	private static String stemLite(String token) {
		if (token.length() > 4 && token.endsWith("s")) {
			return token.substring(0, token.length() - 1);
		}
		return token;
	}
}
