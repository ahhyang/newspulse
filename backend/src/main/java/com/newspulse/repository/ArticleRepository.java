package com.newspulse.repository;

import com.newspulse.domain.Article;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

	boolean existsByUrlHash(String urlHash);

	Optional<Article> findByUrlHash(String urlHash);

	@Query("select max(a.publishedAt) from Article a where a.topic.id = :topicId")
	Optional<Instant> findLatestPublishedAtByTopicId(@Param("topicId") Long topicId);

	@Query("select a.urlHash from Article a where a.urlHash in :hashes")
	Set<String> findExistingHashes(@Param("hashes") Collection<String> hashes);

	@EntityGraph(attributePaths = {"enrichment", "topic", "cluster"})
	Optional<Article> findWithDetailsById(Long id);
}
