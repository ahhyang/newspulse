package com.newspulse.service;

import com.newspulse.config.AppProperties;
import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.dto.EnrichmentRunResponse;
import com.newspulse.llm.LlmClient;
import com.newspulse.llm.LlmClient.EnrichmentResult;
import com.newspulse.repository.ArticleEnrichmentRepository;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.web.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EnrichmentService {

	private static final Logger log = LoggerFactory.getLogger(EnrichmentService.class);

	private final ArticleRepository articleRepository;
	private final ArticleEnrichmentRepository enrichmentRepository;
	private final LlmClient llmClient;
	private final DigestService digestService;
	private final AppProperties appProperties;
	private final TransactionTemplate transactionTemplate;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public EnrichmentService(
			ArticleRepository articleRepository,
			ArticleEnrichmentRepository enrichmentRepository,
			ObjectProvider<LlmClient> llmClient,
			DigestService digestService,
			AppProperties appProperties,
			PlatformTransactionManager transactionManager
	) {
		this.articleRepository = articleRepository;
		this.enrichmentRepository = enrichmentRepository;
		this.llmClient = llmClient.getIfAvailable();
		this.digestService = digestService;
		this.appProperties = appProperties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Async
	public void enrichUnprocessedAsync() {
		try {
			EnrichmentRunResponse result = enrichUnprocessed();
			log.info(
					"Async enrichment finished: scanned={} enriched={} skipped={} failures={}",
					result.scanned(),
					result.enriched(),
					result.skipped(),
					result.failures()
			);
		} catch (ConflictException ex) {
			log.info("Skipping async enrichment: {}", ex.getMessage());
		} catch (Exception ex) {
			log.error("Async enrichment failed without crashing the application", ex);
		}
	}

	public EnrichmentRunResponse enrichUnprocessed() {
		if (!running.compareAndSet(false, true)) {
			throw new ConflictException("An enrichment run is already in progress");
		}
		Instant startedAt = Instant.now();
		int scanned = 0;
		int enriched = 0;
		int skipped = 0;
		int failures = 0;
		try {
			if (llmClient == null) {
				log.warn("No LlmClient bean is registered; enrichment will no-op");
				return new EnrichmentRunResponse(startedAt, Instant.now(), 0, 0, 0, 0);
			}
			int batchSize = Math.max(1, appProperties.enrichment().batchSize());
			List<Article> pending = articleRepository.findUnenriched(PageRequest.of(0, batchSize));
			scanned = pending.size();
			for (Article article : pending) {
				try {
					if (persistIfMissing(article)) {
						enriched++;
					} else {
						skipped++;
					}
				} catch (Exception ex) {
					failures++;
					log.warn(
							"Enrichment failed for article {} ({}): {}",
							article.getId(),
							article.getTitle(),
							ex.getMessage()
					);
					log.debug("Enrichment stacktrace", ex);
				}
			}
			EnrichmentRunResponse response = new EnrichmentRunResponse(startedAt, Instant.now(), scanned, enriched, skipped, failures);
			if (enriched > 0) {
				digestService.generateForTodayAsync();
			}
			return response;
		} finally {
			running.set(false);
		}
	}

	private boolean persistIfMissing(Article article) {
		EnrichmentResult result = llmClient.enrich(article.getTitle(), article.getRawContent());
		Boolean saved = transactionTemplate.execute(status -> {
			if (enrichmentRepository.existsByArticle_Id(article.getId())) {
				return false;
			}
			ArticleEnrichment enrichment = new ArticleEnrichment();
			enrichment.setArticle(article);
			enrichment.setSummary(result.summary());
			enrichment.setSentiment(result.sentiment());
			enrichment.setSentimentJustification(result.justification());
			enrichment.setStanceTag(result.stanceTag());
			enrichment.setModel(result.model());
			try {
				enrichmentRepository.saveAndFlush(enrichment);
				return true;
			} catch (DataIntegrityViolationException ex) {
				log.debug("Skipped duplicate enrichment for article {}", article.getId());
				return false;
			}
		});
		return Boolean.TRUE.equals(saved);
	}
}
