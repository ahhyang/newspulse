package com.newspulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "article_enrichments")
public class ArticleEnrichment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "article_id", nullable = false, unique = true)
	private Article article;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Sentiment sentiment;

	@Column(name = "sentiment_justification", nullable = false, length = 500)
	private String sentimentJustification;

	@Column(name = "stance_tag", length = 64)
	private String stanceTag;

	@Column(nullable = false, length = 120)
	private String model;

	@Column(name = "enriched_at", nullable = false, updatable = false)
	private Instant enrichedAt;

	@PrePersist
	void onCreate() {
		enrichedAt = Instant.now();
	}
}
