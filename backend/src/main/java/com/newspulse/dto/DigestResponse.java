package com.newspulse.dto;

import com.newspulse.domain.Sentiment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DigestResponse(
		Long id,
		Long topicId,
		String topicName,
		LocalDate digestDate,
		String headline,
		String overview,
		BigDecimal positivePct,
		BigDecimal neutralPct,
		BigDecimal negativePct,
		Instant generatedAt,
		List<DigestItemResponse> items
) {
	public record DigestItemResponse(
			int rank,
			String title,
			String summary,
			int sourceCount,
			Sentiment sentiment,
			Long clusterId
	) {}
}
