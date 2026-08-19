package com.newspulse.repository;

import com.newspulse.domain.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	boolean existsByNameIgnoreCase(String name);

	Optional<Topic> findByNameIgnoreCase(String name);

	List<Topic> findAllByActiveTrueOrderByNameAsc();
}
