package com.newspulse.ingestion;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Canonical form used for URL-hash deduplication across sources.
 */
public final class UrlNormalizer {

	private UrlNormalizer() {}

	public static String normalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			return "";
		}
		String trimmed = rawUrl.trim();
		try {
			URI uri = URI.create(trimmed);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			String path = uri.getPath() == null ? "" : uri.getPath();
			if (path.length() > 1 && path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			String query = stripTrackingParams(uri.getQuery());
			StringBuilder normalized = new StringBuilder(host).append(path);
			if (!query.isEmpty()) {
				normalized.append('?').append(query);
			}
			return normalized.toString();
		} catch (IllegalArgumentException ex) {
			return trimmed.toLowerCase(Locale.ROOT);
		}
	}

	private static String stripTrackingParams(String query) {
		if (query == null || query.isBlank()) {
			return "";
		}
		return Arrays.stream(query.split("&"))
				.filter(part -> !part.isBlank())
				.filter(part -> {
					String name = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
					return !name.startsWith("utm_") && !name.equals("fbclid") && !name.equals("gclid");
				})
				.sorted()
				.collect(Collectors.joining("&"));
	}
}
