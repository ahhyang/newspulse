package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Sentiment;
import com.newspulse.dto.StatsResponse;
import com.newspulse.repository.ArticleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

	@Mock
	private ArticleRepository articleRepository;

	private StatsService statsService;

	@BeforeEach
	void setUp() {
		statsService = new StatsService(articleRepository);
	}

	@Test
	void buildsDailySeriesIncludingZeroDays() {
		LocalDate start = LocalDate.of(2026, 8, 17);
		LocalDate end = LocalDate.of(2026, 8, 19);
		Article positive = article(Sentiment.POSITIVE, start);
		Article negative = article(Sentiment.NEGATIVE, end);
		when(articleRepository.findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(eq(1L), any(), any()))
				.thenReturn(List.of(positive, negative));

		StatsResponse response = statsService.sentimentTrend(1L, start, end);

		assertThat(response.articleCount()).isEqualTo(2);
		assertThat(response.series()).hasSize(3);
		assertThat(response.series().get(1).positive()).isZero();
		assertThat(response.series().getFirst().positivePct()).isEqualByComparingTo(new BigDecimal("100.00"));
		assertThat(response.series().getLast().negativePct()).isEqualByComparingTo(new BigDecimal("100.00"));
	}

	private static Article article(Sentiment sentiment, LocalDate day) {
		Article article = new Article();
		article.setPublishedAt(day.atTime(12, 0).toInstant(ZoneOffset.UTC));
		ArticleEnrichment enrichment = new ArticleEnrichment();
		enrichment.setSentiment(sentiment);
		article.setEnrichment(enrichment);
		return article;
	}
}
