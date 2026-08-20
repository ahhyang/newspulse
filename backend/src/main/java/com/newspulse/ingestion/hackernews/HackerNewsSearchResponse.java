package com.newspulse.ingestion.hackernews;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HackerNewsSearchResponse(List<Hit> hits) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Hit(
			String title,
			String url,
			String objectID,
			@JsonProperty("created_at") String createdAt,
			@JsonProperty("story_text") String storyText,
			String author
	) {}
}
