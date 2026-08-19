package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Sentiment;
import com.newspulse.dto.EnrichmentRunResponse;
import com.newspulse.llm.LlmClient;
import com.newspulse.llm.LlmClient.EnrichmentResult;
import com.newspulse.llm.LlmException;
import com.newspulse.repository.ArticleEnrichmentRepository;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.support.AppPropertiesFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private ArticleEnrichmentRepository enrichmentRepository;

	@Mock
	private LlmClient llmClient;

	@Mock
	private ObjectProvider<LlmClient> llmClientProvider;

	private EnrichmentService enrichmentService;

	@BeforeEach
	void setUp() {
		when(llmClientProvider.getIfAvailable()).thenReturn(llmClient);
		enrichmentService = new EnrichmentService(
				articleRepository,
				enrichmentRepository,
				llmClientProvider,
				AppPropertiesFixture.defaults(),
				noopTransactionManager()
		);
	}

	@Test
	void persistsSuccessfulEnrichment() {
		Article article = article(1L, "Good news");
		when(articleRepository.findUnenriched(any(Pageable.class))).thenReturn(List.of(article));
		when(llmClient.enrich(anyString(), any())).thenReturn(
				new EnrichmentResult("A short summary.", Sentiment.POSITIVE, "Upbeat coverage.", "market", "stub")
		);
		when(enrichmentRepository.existsByArticle_Id(1L)).thenReturn(false);
		when(enrichmentRepository.saveAndFlush(any(ArticleEnrichment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		EnrichmentRunResponse result = enrichmentService.enrichUnprocessed();

		assertThat(result.scanned()).isEqualTo(1);
		assertThat(result.enriched()).isEqualTo(1);
		assertThat(result.failures()).isZero();
		verify(enrichmentRepository).saveAndFlush(any(ArticleEnrichment.class));
	}

	@Test
	void llmFailureIsCountedAndDoesNotAbortTheBatch() {
		Article ok = article(1L, "Good news");
		Article bad = article(2L, "Broken news");
		when(articleRepository.findUnenriched(any(Pageable.class))).thenReturn(List.of(ok, bad));
		when(llmClient.enrich("Good news", "body")).thenReturn(
				new EnrichmentResult("Summary", Sentiment.NEUTRAL, "Mixed.", "technical", "stub")
		);
		when(llmClient.enrich("Broken news", "body")).thenThrow(new LlmException("OpenRouter HTTP 500"));
		when(enrichmentRepository.existsByArticle_Id(1L)).thenReturn(false);
		when(enrichmentRepository.saveAndFlush(any(ArticleEnrichment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		EnrichmentRunResponse result = enrichmentService.enrichUnprocessed();

		assertThat(result.scanned()).isEqualTo(2);
		assertThat(result.enriched()).isEqualTo(1);
		assertThat(result.failures()).isEqualTo(1);
		verify(enrichmentRepository, times(1)).saveAndFlush(any(ArticleEnrichment.class));
	}

	@Test
	void skipsWhenEnrichmentAlreadyExists() {
		Article article = article(1L, "Already done");
		when(articleRepository.findUnenriched(any(Pageable.class))).thenReturn(List.of(article));
		when(llmClient.enrich(anyString(), any())).thenReturn(
				new EnrichmentResult("Summary", Sentiment.NEUTRAL, "ok", "other", "stub")
		);
		when(enrichmentRepository.existsByArticle_Id(1L)).thenReturn(true);

		EnrichmentRunResponse result = enrichmentService.enrichUnprocessed();

		assertThat(result.enriched()).isZero();
		assertThat(result.skipped()).isEqualTo(1);
		verify(enrichmentRepository, never()).saveAndFlush(any(ArticleEnrichment.class));
	}

	private static Article article(long id, String title) {
		Article article = new Article();
		article.setId(id);
		article.setTitle(title);
		article.setRawContent("body");
		article.setUrl("https://example.com/" + id);
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
