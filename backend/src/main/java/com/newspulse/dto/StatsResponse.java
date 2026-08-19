package com.newspulse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatsResponse(
		Long topicId,
		LocalDate from,
		LocalDate to,
		long articleCount,
		List<SentimentPoint> series
) {
	public record SentimentPoint(
			LocalDate date,
			long positive,
			long neutral,
			long negative,
			BigDecimal positivePct,
			BigDecimal neutralPct,
			BigDecimal negativePct
	) {}
}
