package com.newspulse.repository;

import com.newspulse.domain.ArticleEnrichment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleEnrichmentRepository extends JpaRepository<ArticleEnrichment, Long> {

	boolean existsByArticle_Id(Long articleId);
}
