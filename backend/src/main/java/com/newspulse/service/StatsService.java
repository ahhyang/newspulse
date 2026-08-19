package com.newspulse.service;

import com.newspulse.domain.Article;
import com.newspulse.domain.Sentiment;
import com.newspulse.dto.StatsResponse;
import com.newspulse.repository.ArticleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

	private final ArticleRepository articleRepository;

	public StatsService(ArticleRepository articleRepository) {
		this.articleRepository = articleRepository;
	}

	public StatsResponse sentimentTrend(Long topicId, LocalDate from, LocalDate to) {
		LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
		LocalDate start = from != null ? from : end.minusDays(6);
		if (start.isAfter(end)) {
			LocalDate swap = start;
			start = end;
			end = swap;
		}

		var fromInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
		var toInstant = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<Article> articles = (topicId == null
				? articleRepository.findByPublishedAtGreaterThanEqualAndPublishedAtLessThan(fromInstant, toInstant)
				: articleRepository.findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(topicId, fromInstant, toInstant))
				.stream()
				.filter(article -> article.getEnrichment() != null && article.getPublishedAt() != null)
				.toList();

		Map<LocalDate, EnumMap<Sentiment, Long>> byDay = new java.util.TreeMap<>();
		for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
			byDay.put(day, emptyCounts());
		}
		for (Article article : articles) {
			LocalDate day = article.getPublishedAt().atZone(ZoneOffset.UTC).toLocalDate();
			EnumMap<Sentiment, Long> counts = byDay.computeIfAbsent(day, ignored -> emptyCounts());
			counts.merge(article.getEnrichment().getSentiment(), 1L, Long::sum);
		}

		List<StatsResponse.SentimentPoint> series = new ArrayList<>();
		long total = 0;
		for (var entry : byDay.entrySet()) {
			EnumMap<Sentiment, Long> counts = entry.getValue();
			long positive = counts.get(Sentiment.POSITIVE);
			long neutral = counts.get(Sentiment.NEUTRAL);
			long negative = counts.get(Sentiment.NEGATIVE);
			long dayTotal = positive + neutral + negative;
			total += dayTotal;
			series.add(new StatsResponse.SentimentPoint(
					entry.getKey(),
					positive,
					neutral,
					negative,
					pct(positive, dayTotal),
					pct(neutral, dayTotal),
					pct(negative, dayTotal)
			));
		}
		return new StatsResponse(topicId, start, end, total, series);
	}

	private static EnumMap<Sentiment, Long> emptyCounts() {
		EnumMap<Sentiment, Long> counts = new EnumMap<>(Sentiment.class);
		counts.put(Sentiment.POSITIVE, 0L);
		counts.put(Sentiment.NEUTRAL, 0L);
		counts.put(Sentiment.NEGATIVE, 0L);
		return counts;
	}

	private static BigDecimal pct(long part, long total) {
		if (total == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
		}
		return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
	}
}
