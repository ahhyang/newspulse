package com.newspulse.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Sentiment;
import com.newspulse.domain.Topic;
import com.newspulse.dto.DigestResponse;
import com.newspulse.dto.StatsResponse;
import com.newspulse.repository.ArticleEnrichmentRepository;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.TopicRepository;
import com.newspulse.service.DigestService;
import com.newspulse.service.StatsService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DigestPipelineIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private DigestService digestService;

	@Autowired
	private StatsService statsService;

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private ArticleEnrichmentRepository enrichmentRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Test
	void clustersNearDuplicatesAndExposesSentimentStats() {
		Topic topic = topicRepository.findByNameIgnoreCase("AI Industry").orElseThrow();
		LocalDate day = LocalDate.of(2026, 8, 18);
		Instant morning = day.atTime(8, 0).toInstant(ZoneOffset.UTC);
		save(topic, "h1", "OpenAI releases GPT-5 model to API", "https://a.example/1", "Reuters", Sentiment.POSITIVE, morning);
		save(topic, "h2", "OpenAI releases GPT-5 to the API", "https://b.example/2", "Bloomberg", Sentiment.POSITIVE, morning.plusSeconds(3600));
		save(topic, "h3", "EU probes foundation model training data", "https://c.example/3", "Policy Daily", Sentiment.NEGATIVE, morning.plusSeconds(7200));

		DigestResponse digest = digestService.generateOne(topic.getId(), day);

		assertThat(digest.items()).hasSize(2);
		assertThat(digest.items().getFirst().sourceCount()).isEqualTo(2);
		assertThat(digest.positivePct()).isPositive();
		assertThat(digestService.getByDate(day, topic.getId()).id()).isEqualTo(digest.id());
		assertThat(digestService.getLatest(topic.getId()).id()).isEqualTo(digest.id());

		StatsResponse stats = statsService.sentimentTrend(topic.getId(), day, day);
		assertThat(stats.articleCount()).isEqualTo(3);
		assertThat(stats.series()).hasSize(1);
		assertThat(stats.series().getFirst().positive()).isEqualTo(2);
		assertThat(stats.series().getFirst().negative()).isEqualTo(1);
	}

	private void save(
			Topic topic,
			String hash,
			String title,
			String url,
			String sourceName,
			Sentiment sentiment,
			Instant publishedAt
	) {
		Article article = new Article();
		article.setTopic(topic);
		article.setTitle(title);
		article.setUrl(url);
		article.setUrlHash(hash);
		article.setSource("gnews");
		article.setSourceName(sourceName);
		article.setPublishedAt(publishedAt);
		article.setRawContent("Body of " + title);
		article = articleRepository.saveAndFlush(article);

		ArticleEnrichment enrichment = new ArticleEnrichment();
		enrichment.setArticle(article);
		enrichment.setSummary("Summary of " + title);
		enrichment.setSentiment(sentiment);
		enrichment.setSentimentJustification("test");
		enrichment.setStanceTag("technical");
		enrichment.setModel("stub");
		enrichmentRepository.saveAndFlush(enrichment);
	}
}
