package com.newspulse.dto;

import java.util.List;

public record BatchSummaryResponse(
		String headline,
		String overview,
		List<String> themes,
		int articleCount,
		List<ArticleRef> articles,
		String model
) {
	public record ArticleRef(Long id, String title, String sourceName) {}
}
