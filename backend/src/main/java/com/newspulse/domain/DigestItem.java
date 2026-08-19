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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "digest_items")
public class DigestItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "digest_id", nullable = false)
	private Digest digest;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cluster_id")
	private StoryCluster cluster;

	@Column(nullable = false)
	private int rank;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String summary;

	@Column(name = "source_count", nullable = false)
	private int sourceCount = 1;

	@Enumerated(EnumType.STRING)
	@Column(length = 16)
	private Sentiment sentiment;
}
