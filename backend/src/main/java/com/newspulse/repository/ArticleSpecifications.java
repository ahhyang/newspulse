package com.newspulse.repository;

import com.newspulse.domain.Article;
import com.newspulse.domain.Sentiment;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ArticleSpecifications {

	private ArticleSpecifications() {}

	public static Specification<Article> withFilters(
			Long topicId,
			String source,
			String sourceName,
			Long clusterId,
			Sentiment sentiment,
			Instant from,
			Instant to
	) {
		return (root, query, cb) -> {
			if (query != null && Article.class.equals(query.getResultType())) {
				root.fetch("enrichment", JoinType.LEFT);
				root.fetch("topic", JoinType.LEFT);
				query.distinct(true);
			}

			List<Predicate> predicates = new ArrayList<>();
			if (topicId != null) {
				predicates.add(cb.equal(root.get("topic").get("id"), topicId));
			}
			if (clusterId != null) {
				predicates.add(cb.equal(root.get("cluster").get("id"), clusterId));
			}
			if (source != null && !source.isBlank()) {
				predicates.add(cb.equal(cb.lower(root.get("source")), source.toLowerCase()));
			}
			if (sourceName != null && !sourceName.isBlank()) {
				predicates.add(cb.equal(cb.lower(root.get("sourceName")), sourceName.toLowerCase()));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"), from));
			}
			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("publishedAt"), to));
			}
			if (sentiment != null) {
				predicates.add(cb.equal(root.join("enrichment", JoinType.INNER).get("sentiment"), sentiment));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
