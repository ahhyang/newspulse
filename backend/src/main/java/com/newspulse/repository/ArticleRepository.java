package com.newspulse.repository;

import com.newspulse.domain.Article;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

	boolean existsByUrlHash(String urlHash);

	Optional<Article> findByUrlHash(String urlHash);

	@EntityGraph(attributePaths = {"enrichment", "topic", "cluster"})
	Optional<Article> findWithDetailsById(Long id);
}
