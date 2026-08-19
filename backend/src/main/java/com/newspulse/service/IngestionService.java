package com.newspulse.service;

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
import com.newspulse.web.ConflictException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IngestionService {

	private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

	private final TopicRepository topicRepository;
	private final ArticleRepository articleRepository;
	private final List<NewsSource> newsSources;
	private final AppProperties appProperties;
	private final TransactionTemplate transactionTemplate;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public IngestionService(
			TopicRepository topicRepository,
			ArticleRepository articleRepository,
			List<NewsSource> newsSources,
			AppProperties appProperties,
			PlatformTransactionManager transactionManager
	) {
		this.topicRepository = topicRepository;
		this.articleRepository = articleRepository;
		this.newsSources = newsSources;
		this.appProperties = appProperties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public IngestionRunResponse ingestAll() {
		if (!running.compareAndSet(false, true)) {
			throw new ConflictException("An ingestion run is already in progress");
		}
		Instant startedAt = Instant.now();
		int fetched = 0;
		int inserted = 0;
		int duplicates = 0;
		int skippedInvalid = 0;
		int sourceFailures = 0;
		List<Topic> topics = topicRepository.findAllByActiveTrueOrderByNameAsc();
		try {
			if (newsSources.isEmpty()) {
				log.warn("No NewsSource beans are registered; ingestion will no-op");
			}
			for (Topic topic : topics) {
				Instant since = resolveSince(topic);
				for (NewsSource source : newsSources) {
					try {
						List<RawArticle> articles = source.fetch(topic, since);
						fetched += articles.size();
						PersistStats stats = persist(topic, articles);
						inserted += stats.inserted;
						duplicates += stats.duplicates;
						skippedInvalid += stats.skippedInvalid;
					} catch (Exception ex) {
						sourceFailures++;
						log.warn(
								"Ingestion failed for source '{}' topic '{}': {}",
								source.id(),
								topic.getName(),
								ex.getMessage()
						);
						log.debug("Ingestion source stacktrace", ex);
					}
				}
			}
			return new IngestionRunResponse(
					startedAt,
					Instant.now(),
					topics.size(),
					fetched,
					inserted,
					duplicates,
					skippedInvalid,
					sourceFailures
			);
		} finally {
			running.set(false);
		}
	}

	private Instant resolveSince(Topic topic) {
		return articleRepository.findLatestPublishedAtByTopicId(topic.getId())
				.orElseGet(() -> Instant.now().minus(appProperties.ingestion().lookbackHours(), ChronoUnit.HOURS));
	}

	private PersistStats persist(Topic topic, List<RawArticle> articles) {
		PersistStats stats = transactionTemplate.execute(status -> persistInTransaction(topic, articles));
		return stats != null ? stats : new PersistStats(0, 0, 0);
	}

	private PersistStats persistInTransaction(Topic topic, List<RawArticle> articles) {
		int inserted = 0;
		int duplicates = 0;
		int skippedInvalid = 0;
		List<Article> toInsert = new ArrayList<>();
		Set<String> hashesThisBatch = new HashSet<>();

		for (RawArticle raw : articles) {
			if (raw.url() == null || raw.url().isBlank() || raw.title() == null || raw.title().isBlank()) {
				skippedInvalid++;
				continue;
			}
			String urlHash = Hashes.sha256(UrlNormalizer.normalize(raw.url()));
			if (!hashesThisBatch.add(urlHash)) {
				duplicates++;
				continue;
			}
			Article article = new Article();
			article.setTopic(topic);
			article.setTitle(truncate(raw.title(), 1000));
			article.setUrl(truncate(raw.url(), 2048));
			article.setUrlHash(urlHash);
			article.setSource(truncate(raw.sourceId(), 80));
			article.setSourceName(truncate(raw.sourceName() == null ? raw.sourceId() : raw.sourceName(), 160));
			article.setPublishedAt(raw.publishedAt());
			article.setRawContent(raw.rawContent());
			article.setContentHash(Hashes.contentHash(raw.rawContent()));
			toInsert.add(article);
		}

		Set<String> existing = toInsert.isEmpty()
				? Set.of()
				: articleRepository.findExistingHashes(toInsert.stream().map(Article::getUrlHash).toList());

		List<Article> fresh = new ArrayList<>();
		for (Article article : toInsert) {
			if (existing.contains(article.getUrlHash())) {
				duplicates++;
			} else {
				fresh.add(article);
			}
		}

		for (Article article : fresh) {
			try {
				articleRepository.saveAndFlush(article);
				inserted++;
			} catch (DataIntegrityViolationException ex) {
				duplicates++;
				log.debug("Skipped duplicate URL hash {}", article.getUrlHash());
			}
		}
		return new PersistStats(inserted, duplicates, skippedInvalid);
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		String stripped = value.strip();
		return stripped.length() <= max ? stripped : stripped.substring(0, max);
	}

	private record PersistStats(int inserted, int duplicates, int skippedInvalid) {}
}
