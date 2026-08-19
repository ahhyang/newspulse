package com.newspulse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "articles")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cluster_id")
	private StoryCluster cluster;

	@Column(nullable = false, length = 1000)
	private String title;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(name = "url_hash", nullable = false, unique = true, length = 64)
	private String urlHash;

	@Column(nullable = false, length = 80)
	private String source;

	@Column(name = "source_name", nullable = false, length = 160)
	private String sourceName;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "raw_content", columnDefinition = "TEXT")
	private String rawContent;

	@Column(name = "content_hash", length = 64)
	private String contentHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToOne(mappedBy = "article", fetch = FetchType.LAZY)
	private ArticleEnrichment enrichment;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}
