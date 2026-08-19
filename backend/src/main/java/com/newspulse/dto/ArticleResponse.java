package com.newspulse.dto;

import com.newspulse.domain.Sentiment;
import java.time.Instant;

public record ArticleResponse(
		Long id,
		Long topicId,
		String topicName,
		Long clusterId,
		String title,
		String url,
		String source,
		String sourceName,
		Instant publishedAt,
		String summary,
		Sentiment sentiment,
		String sentimentJustification,
		String stanceTag
) {}
