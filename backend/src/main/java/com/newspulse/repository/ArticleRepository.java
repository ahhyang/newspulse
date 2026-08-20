package com.newspulse.repository;

import com.newspulse.domain.Article;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
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

	@Query("""
			select a from Article a
			where not exists (select 1 from ArticleEnrichment e where e.article = a)
			order by a.publishedAt desc nulls last, a.id desc
			""")
	List<Article> findUnenriched(Pageable pageable);

	@EntityGraph(attributePaths = {"enrichment", "topic"})
	List<Article> findAllByIdIn(Collection<Long> ids);

	@EntityGraph(attributePaths = {"enrichment", "topic", "cluster"})
	Optional<Article> findWithDetailsById(Long id);

	@EntityGraph(attributePaths = {"enrichment", "topic", "cluster"})
	List<Article> findByTopicIdAndClusterIsNull(Long topicId);

	@EntityGraph(attributePaths = {"enrichment", "topic", "cluster"})
	List<Article> findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(
			Long topicId,
			Instant from,
			Instant to
	);

	@EntityGraph(attributePaths = {"enrichment", "topic"})
	List<Article> findByPublishedAtGreaterThanEqualAndPublishedAtLessThan(Instant from, Instant to);

}
