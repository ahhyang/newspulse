package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newspulse.config.AppProperties;
import com.newspulse.domain.Article;
import com.newspulse.domain.Topic;
import com.newspulse.dto.IngestionRunResponse;
import com.newspulse.ingestion.Hashes;
import com.newspulse.ingestion.NewsSource;
import com.newspulse.ingestion.NewsSource.RawArticle;
import com.newspulse.ingestion.UrlNormalizer;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

	@Mock
	private TopicRepository topicRepository;

	@Mock
	private ArticleRepository articleRepository;

	private Topic topic;
	private IngestionService ingestionService;

	@BeforeEach
	void setUp() {
		topic = new Topic("AI Industry", "artificial intelligence", "desc");
		topic.setId(1L);
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("test-jwt-secret-key-that-is-long-enough-for-hs256-algorithms", 3600000),
				new AppProperties.Admin("admin", "s3cret"),
				new AppProperties.Cors("http://localhost:5173"),
				new AppProperties.Ingestion(true, 3600000, 20000, 24),
				new AppProperties.Llm("openrouter", "https://openrouter.ai/api/v1", "test", "model", "https://example.com", "NewsPulse"),
				new AppProperties.Gnews(true, "key", "https://gnews.io/api/v4", "en", 10)
		);
		NewsSource source = new NewsSource() {
			@Override
			public String id() {
				return "gnews";
			}

			@Override
			public List<RawArticle> fetch(Topic topicArg, Instant since) {
				return List.of(
						new RawArticle(
								"First story",
								"https://news.example.com/a",
								"gnews",
								"Example",
								Instant.parse("2026-08-18T10:00:00Z"),
								"body a"
						),
						new RawArticle(
								"First story again",
								"https://news.example.com/a?utm_source=rss",
								"gnews",
								"Example",
								Instant.parse("2026-08-18T11:00:00Z"),
								"body a copy"
						),
						new RawArticle(
								"Second story",
								"https://other.example.com/b",
								"gnews",
								"Other",
								Instant.parse("2026-08-18T12:00:00Z"),
								"body b"
						)
				);
			}
		};
		ingestionService = new IngestionService(
				topicRepository,
				articleRepository,
				List.of(source),
				properties,
				noopTransactionManager()
		);
	}

	@Test
	void insertsNewArticlesAndSkipsNormalizedUrlDuplicates() {
		when(topicRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(topic));
		when(articleRepository.findLatestPublishedAtByTopicId(1L)).thenReturn(Optional.empty());
		when(articleRepository.findExistingHashes(anyCollection())).thenReturn(Set.of());
		when(articleRepository.saveAndFlush(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IngestionRunResponse result = ingestionService.ingestAll();

		assertThat(result.fetched()).isEqualTo(3);
		assertThat(result.inserted()).isEqualTo(2);
		assertThat(result.duplicates()).isEqualTo(1);
		assertThat(result.sourceFailures()).isZero();

		ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
		verify(articleRepository, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
		assertThat(captor.getAllValues())
				.extracting(Article::getUrl)
				.containsExactlyInAnyOrder("https://news.example.com/a", "https://other.example.com/b");
	}

	@Test
	void skipsUrlsAlreadyStored() {
		when(topicRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(topic));
		when(articleRepository.findLatestPublishedAtByTopicId(1L)).thenReturn(Optional.empty());
		String existingHash = Hashes.sha256(UrlNormalizer.normalize("https://news.example.com/a"));
		when(articleRepository.findExistingHashes(anyCollection())).thenReturn(Set.of(existingHash));
		when(articleRepository.saveAndFlush(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

		IngestionRunResponse result = ingestionService.ingestAll();

		assertThat(result.inserted()).isEqualTo(1);
		assertThat(result.duplicates()).isEqualTo(2);
		verify(articleRepository, org.mockito.Mockito.times(1)).saveAndFlush(any(Article.class));
	}

	@Test
	void sourceFailureIsCountedAndDoesNotAbortOtherWork() {
		NewsSource failing = new NewsSource() {
			@Override
			public String id() {
				return "gnews";
			}

			@Override
			public List<RawArticle> fetch(Topic topicArg, Instant since) {
				throw new IllegalStateException("GNews down");
			}
		};
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("test-jwt-secret-key-that-is-long-enough-for-hs256-algorithms", 3600000),
				new AppProperties.Admin("admin", "s3cret"),
				new AppProperties.Cors("http://localhost:5173"),
				new AppProperties.Ingestion(true, 3600000, 20000, 24),
				new AppProperties.Llm("openrouter", "https://openrouter.ai/api/v1", "test", "model", "https://example.com", "NewsPulse"),
				new AppProperties.Gnews(true, "key", "https://gnews.io/api/v4", "en", 10)
		);
		ingestionService = new IngestionService(
				topicRepository,
				articleRepository,
				List.of(failing),
				properties,
				noopTransactionManager()
		);
		when(topicRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(topic));
		when(articleRepository.findLatestPublishedAtByTopicId(1L)).thenReturn(Optional.empty());

		IngestionRunResponse result = ingestionService.ingestAll();

		assertThat(result.sourceFailures()).isEqualTo(1);
		assertThat(result.inserted()).isZero();
		verify(articleRepository, never()).saveAndFlush(any(Article.class));
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
