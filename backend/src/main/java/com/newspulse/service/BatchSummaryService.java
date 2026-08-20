package com.newspulse.service;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.dto.BatchSummaryRequest;
import com.newspulse.dto.BatchSummaryResponse;
import com.newspulse.llm.LlmClient;
import com.newspulse.llm.LlmException;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.web.NotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchSummaryService {

	private static final int MAX_ARTICLES = 12;

	private final ArticleRepository articleRepository;
	private final LlmClient llmClient;

	public BatchSummaryService(ArticleRepository articleRepository, LlmClient llmClient) {
		this.articleRepository = articleRepository;
		this.llmClient = llmClient;
	}

	@Transactional(readOnly = true)
	public BatchSummaryResponse summarize(BatchSummaryRequest request) {
		List<Long> ids = request.articleIds().stream().distinct().limit(MAX_ARTICLES).toList();
		if (ids.size() < 2) {
			throw new IllegalArgumentException("Select at least 2 articles to summarize");
		}

		Map<Long, Article> byId = new LinkedHashMap<>();
		for (Article article : articleRepository.findAllByIdIn(ids)) {
			byId.put(article.getId(), article);
		}
		if (byId.size() != ids.size()) {
			throw new NotFoundException("One or more selected articles were not found");
		}

		List<LlmClient.BatchArticle> briefs = ids.stream().map(id -> toBrief(byId.get(id))).toList();
		try {
			LlmClient.BatchSummaryResult result = llmClient.summarizeBatch(briefs);
			List<BatchSummaryResponse.ArticleRef> refs = ids.stream()
					.map(id -> {
						Article article = byId.get(id);
						return new BatchSummaryResponse.ArticleRef(article.getId(), article.getTitle(), article.getSourceName());
					})
					.toList();
			return new BatchSummaryResponse(
					result.headline(),
					result.overview(),
					result.themes(),
					refs.size(),
					refs,
					result.model()
			);
		} catch (LlmException ex) {
			throw ex;
		}
	}

	private static LlmClient.BatchArticle toBrief(Article article) {
		ArticleEnrichment enrichment = article.getEnrichment();
		String snippet = firstNonBlank(
				enrichment == null ? null : enrichment.getSummary(),
				article.getRawContent(),
				article.getTitle()
		);
		String sentiment = enrichment == null || enrichment.getSentiment() == null
				? "UNSCORED"
				: enrichment.getSentiment().name();
		return new LlmClient.BatchArticle(article.getTitle(), article.getSourceName(), snippet, sentiment);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				String trimmed = value.strip();
				return trimmed.length() > 600 ? trimmed.substring(0, 600) : trimmed;
			}
		}
		return "";
	}
}
