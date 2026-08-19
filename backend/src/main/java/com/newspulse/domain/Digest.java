package com.newspulse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "digests")
public class Digest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@Column(name = "digest_date", nullable = false)
	private LocalDate digestDate;

	@Column(nullable = false, length = 500)
	private String headline;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String overview;

	@Column(name = "positive_pct", nullable = false, precision = 5, scale = 2)
	private BigDecimal positivePct = BigDecimal.ZERO;

	@Column(name = "neutral_pct", nullable = false, precision = 5, scale = 2)
	private BigDecimal neutralPct = BigDecimal.ZERO;

	@Column(name = "negative_pct", nullable = false, precision = 5, scale = 2)
	private BigDecimal negativePct = BigDecimal.ZERO;

	@Column(name = "generated_at", nullable = false, updatable = false)
	private Instant generatedAt;

	@OneToMany(mappedBy = "digest", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("rank ASC")
	private List<DigestItem> items = new ArrayList<>();

	@PrePersist
	void onCreate() {
		generatedAt = Instant.now();
	}
}
