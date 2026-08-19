package com.newspulse.service;

import com.newspulse.dto.StatsResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

	public StatsResponse sentimentTrend(Long topicId, LocalDate from, LocalDate to) {
		LocalDate end = to != null ? to : LocalDate.now();
		LocalDate start = from != null ? from : end.minusDays(7);
		return new StatsResponse(topicId, start, end, 0, List.of(
				new StatsResponse.SentimentPoint(
						end,
						0,
						0,
						0,
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						BigDecimal.ZERO
				)
		));
	}
}
