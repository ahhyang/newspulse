package com.newspulse.mapper;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Digest;
import com.newspulse.domain.DigestItem;
import com.newspulse.domain.Topic;
import com.newspulse.dto.ArticleResponse;
import com.newspulse.dto.DigestResponse;
import com.newspulse.dto.TopicResponse;
import java.util.List;

public final class NewsPulseMapper {

	private NewsPulseMapper() {}

	public static TopicResponse toResponse(Topic topic) {
		return new TopicResponse(
				topic.getId(),
				topic.getName(),
				topic.getQuery(),
				topic.getDescription(),
				topic.isActive(),
				topic.getCreatedAt(),
				topic.getUpdatedAt()
		);
	}

	public static ArticleResponse toResponse(Article article) {
		ArticleEnrichment enrichment = article.getEnrichment();
		return new ArticleResponse(
				article.getId(),
				article.getTopic() != null ? article.getTopic().getId() : null,
				article.getTopic() != null ? article.getTopic().getName() : null,
				article.getCluster() != null ? article.getCluster().getId() : null,
				article.getTitle(),
				article.getUrl(),
				article.getSource(),
				article.getSourceName(),
				article.getPublishedAt(),
				enrichment != null ? enrichment.getSummary() : null,
				enrichment != null ? enrichment.getSentiment() : null,
				enrichment != null ? enrichment.getSentimentJustification() : null,
				enrichment != null ? enrichment.getStanceTag() : null
		);
	}

	public static DigestResponse toResponse(Digest digest) {
		List<DigestResponse.DigestItemResponse> items = digest.getItems().stream()
				.map(NewsPulseMapper::toResponse)
				.toList();
		return new DigestResponse(
				digest.getId(),
				digest.getTopic() != null ? digest.getTopic().getId() : null,
				digest.getTopic() != null ? digest.getTopic().getName() : null,
				digest.getDigestDate(),
				digest.getHeadline(),
				digest.getOverview(),
				digest.getPositivePct(),
				digest.getNeutralPct(),
				digest.getNegativePct(),
				digest.getGeneratedAt(),
				items
		);
	}

	private static DigestResponse.DigestItemResponse toResponse(DigestItem item) {
		return new DigestResponse.DigestItemResponse(
				item.getRank(),
				item.getTitle(),
				item.getSummary(),
				item.getSourceCount(),
				item.getSentiment(),
				item.getCluster() != null ? item.getCluster().getId() : null
		);
	}
}
