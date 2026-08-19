package com.newspulse.repository;

import com.newspulse.domain.Digest;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestRepository extends JpaRepository<Digest, Long> {

	@EntityGraph(attributePaths = {"items", "topic"})
	Optional<Digest> findByTopicIdAndDigestDate(Long topicId, LocalDate digestDate);

	@EntityGraph(attributePaths = {"items", "topic"})
	Optional<Digest> findFirstByOrderByDigestDateDescGeneratedAtDesc();

	@EntityGraph(attributePaths = {"items", "topic"})
	Optional<Digest> findFirstByTopicIdOrderByDigestDateDescGeneratedAtDesc(Long topicId);
}
