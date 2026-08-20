package com.newspulse.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.newspulse.domain.Article;
import com.newspulse.domain.Sentiment;
import com.newspulse.domain.Topic;
import com.newspulse.dto.EnrichmentRunResponse;
import com.newspulse.llm.LlmClient;
import com.newspulse.llm.LlmClient.BatchArticle;
import com.newspulse.llm.LlmClient.BatchSummaryResult;
import com.newspulse.llm.LlmClient.EnrichmentResult;
import com.newspulse.llm.LlmException;
import com.newspulse.repository.ArticleEnrichmentRepository;
import java.util.List;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.TopicRepository;
import com.newspulse.service.EnrichmentService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@ActiveProfiles("test")
@Import(EnrichmentPipelineIT.StubLlmConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class EnrichmentPipelineIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private EnrichmentService enrichmentService;

	@Autowired
	private ArticleRepository articleRepository;

	@Autowired
	private ArticleEnrichmentRepository enrichmentRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Test
	void enrichesPendingArticlesAndIsolatesLlmFailures() {
		Topic topic = topicRepository.findByNameIgnoreCase("AI Industry").orElseThrow();
		articleRepository.save(article(topic, "ok-1", "Labs expand compute", "https://news.example.com/ok"));
		articleRepository.save(article(topic, "fail-1", "FAIL this one", "https://news.example.com/fail"));

		EnrichmentRunResponse result = enrichmentService.enrichUnprocessed();

		assertThat(result.scanned()).isEqualTo(2);
		assertThat(result.enriched()).isEqualTo(1);
		assertThat(result.failures()).isEqualTo(1);
		assertThat(enrichmentRepository.count()).isEqualTo(1);
		assertThat(enrichmentRepository.findAll().getFirst().getSentiment()).isEqualTo(Sentiment.POSITIVE);
	}

	private static Article article(Topic topic, String hash, String title, String url) {
		Article article = new Article();
		article.setTopic(topic);
		article.setTitle(title);
		article.setUrl(url);
		article.setUrlHash(hash);
		article.setSource("gnews");
		article.setSourceName("Example");
		article.setRawContent("Body for " + title);
		return article;
	}

	@TestConfiguration
	static class StubLlmConfig {

		@Bean
		@Primary
		LlmClient stubLlmClient() {
			return new LlmClient() {
				@Override
				public EnrichmentResult enrich(String title, String content) {
					if (title != null && title.startsWith("FAIL")) {
						throw new LlmException("simulated OpenRouter outage");
					}
					return new EnrichmentResult(
							"Two sentence summary of the story. It is useful for the digest.",
							Sentiment.POSITIVE,
							"Positive coverage tone.",
							"product",
							"stub"
					);
				}

				@Override
				public BatchSummaryResult summarizeBatch(List<BatchArticle> articles) {
					return new BatchSummaryResult("Stub headline", "Stub overview.", List.of("stub"), "stub");
				}
			};
		}
	}
}
