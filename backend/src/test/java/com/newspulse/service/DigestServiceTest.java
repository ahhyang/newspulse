package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newspulse.clustering.StoryClusteringService;
import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Digest;
import com.newspulse.domain.Sentiment;
import com.newspulse.domain.StoryCluster;
import com.newspulse.domain.Topic;
import com.newspulse.dto.DigestResponse;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.DigestRepository;
import com.newspulse.repository.TopicRepository;
import com.newspulse.support.AppPropertiesFixture;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class DigestServiceTest {

	@Mock
	private DigestRepository digestRepository;

	@Mock
	private TopicRepository topicRepository;

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private StoryClusteringService clusteringService;

	private DigestService digestService;
	private Topic topic;

	@BeforeEach
	void setUp() {
		topic = new Topic("AI Industry", "ai", "desc");
		topic.setId(1L);
		digestService = new DigestService(
				digestRepository,
				topicRepository,
				articleRepository,
				clusteringService,
				AppPropertiesFixture.defaults(),
				noopTransactionManager()
		);
	}

	@Test
	void generateOneRanksSharedStoriesAndComputesSentimentMix() {
		when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
		when(clusteringService.clusterTopic(topic)).thenReturn(2);
		when(digestRepository.findByTopicIdAndDigestDate(any(), any())).thenReturn(Optional.empty());
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> {
			Digest digest = invocation.getArgument(0);
			digest.setId(99L);
			digest.setGeneratedAt(Instant.parse("2026-08-19T00:00:00Z"));
			return digest;
		});

		StoryCluster cluster = new StoryCluster();
		cluster.setId(5L);
		cluster.setTopic(topic);
		cluster.setCanonicalTitle("Model launch");
		LocalDate day = LocalDate.of(2026, 8, 18);
		Instant morning = day.atTime(9, 0).toInstant(ZoneOffset.UTC);
		Instant noon = day.atTime(12, 0).toInstant(ZoneOffset.UTC);

		Article reuters = article(1L, "OpenAI releases GPT-5", "Reuters", Sentiment.POSITIVE, cluster, morning);
		Article bloomberg = article(2L, "OpenAI GPT-5 hits API", "Bloomberg", Sentiment.POSITIVE, cluster, noon);
		Article regulator = article(3L, "EU probes model training data", "Policy Daily", Sentiment.NEGATIVE, null, noon);
		when(articleRepository.findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(any(), any(), any()))
				.thenReturn(List.of(reuters, bloomberg, regulator));

		DigestResponse response = digestService.generateOne(1L, day);

		assertThat(response.items()).hasSize(2);
		assertThat(response.items().getFirst().sourceCount()).isEqualTo(2);
		assertThat(response.items().getFirst().sentiment()).isEqualTo(Sentiment.POSITIVE);
		assertThat(response.positivePct()).isEqualByComparingTo(new BigDecimal("66.67"));
		assertThat(response.negativePct()).isEqualByComparingTo(new BigDecimal("33.33"));
		assertThat(response.overview()).contains("2 distinct stories").contains("66.67% positive");
	}

	@Test
	void generateOneWritesPlaceholderWhenNoEnrichedArticles() {
		when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
		when(clusteringService.clusterTopic(topic)).thenReturn(0);
		when(digestRepository.findByTopicIdAndDigestDate(any(), any())).thenReturn(Optional.empty());
		when(digestRepository.save(any(Digest.class))).thenAnswer(invocation -> {
			Digest digest = invocation.getArgument(0);
			digest.setId(7L);
			digest.setGeneratedAt(Instant.parse("2026-08-19T00:00:00Z"));
			return digest;
		});
		when(articleRepository.findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(any(), any(), any()))
				.thenReturn(List.of());

		DigestResponse response = digestService.generateOne(1L, LocalDate.of(2026, 8, 18));

		assertThat(response.items()).isEmpty();
		assertThat(response.headline()).contains("AI Industry briefing");
		assertThat(response.overview()).isEqualTo("No enriched articles were available for this day.");
		assertThat(response.positivePct()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
	}

	private Article article(
			long id,
			String title,
			String sourceName,
			Sentiment sentiment,
			StoryCluster cluster,
			Instant publishedAt
	) {
		Article article = new Article();
		article.setId(id);
		article.setTopic(topic);
		article.setTitle(title);
		article.setUrl("https://example.com/" + id);
		article.setSource("gnews");
		article.setSourceName(sourceName);
		article.setPublishedAt(publishedAt);
		article.setCluster(cluster);
		ArticleEnrichment enrichment = new ArticleEnrichment();
		enrichment.setArticle(article);
		enrichment.setSummary("Summary of " + title);
		enrichment.setSentiment(sentiment);
		enrichment.setSentimentJustification("because");
		enrichment.setModel("stub");
		article.setEnrichment(enrichment);
		return article;
	}

	private static PlatformTransactionManager noopTransactionManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) {
			}

			@Override
			public void rollback(TransactionStatus status) {
			}
		};
	}
}
