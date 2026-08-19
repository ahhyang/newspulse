package com.newspulse.ingestion.gnews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GnewsSearchResponse(Integer totalArticles, List<GnewsArticle> articles) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GnewsArticle(
			String title,
			String description,
			String content,
			String url,
			String publishedAt,
			GnewsSource source
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GnewsSource(String name, String url) {}
}
