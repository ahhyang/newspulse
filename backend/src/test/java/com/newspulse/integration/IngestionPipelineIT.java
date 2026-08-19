package com.newspulse.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.newspulse.domain.Topic;
import com.newspulse.dto.IngestionRunResponse;
import com.newspulse.ingestion.NewsSource;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.TopicRepository;
import com.newspulse.service.IngestionService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Import(IngestionPipelineIT.StubNewsSourceConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class IngestionPipelineIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private IngestionService ingestionService;

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Test
	void persistsArticlesThenDeduplicatesOnSecondRun() {
		Topic topic = topicRepository.findByNameIgnoreCase("AI Industry").orElseThrow();
		assertThat(topic.isActive()).isTrue();

		IngestionRunResponse first = ingestionService.ingestAll();
		assertThat(first.sourceFailures()).isZero();
		assertThat(first.inserted()).isEqualTo(2);
		assertThat(articleRepository.count()).isEqualTo(2);

		IngestionRunResponse second = ingestionService.ingestAll();
		assertThat(second.inserted()).isZero();
		assertThat(second.duplicates()).isGreaterThanOrEqualTo(2);
		assertThat(articleRepository.count()).isEqualTo(2);
		assertThat(articleRepository.findAll())
				.extracting(article -> article.getSource())
				.containsOnly("gnews");
	}

	@TestConfiguration
	static class StubNewsSourceConfig {

		@Bean
		@Primary
		NewsSource stubGnewsSource() {
			AtomicInteger calls = new AtomicInteger();
			return new NewsSource() {
				@Override
				public String id() {
					return "gnews";
				}

				@Override
				public List<RawArticle> fetch(Topic topic, Instant since) {
					calls.incrementAndGet();
					return List.of(
							new RawArticle(
									"Model weights leak rumors",
									"https://news.example.com/weights",
									"gnews",
									"Example News",
									Instant.parse("2026-08-18T09:00:00Z"),
									"Rumors about leaked weights."
							),
							new RawArticle(
									"Regulators propose AI labeling",
									"https://news.example.com/labeling",
									"gnews",
									"Policy Daily",
									Instant.parse("2026-08-18T10:00:00Z"),
									"A new labeling rule is on the table."
							)
					);
				}
			};
		}
	}
}
