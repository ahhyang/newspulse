package com.newspulse.repository;

import com.newspulse.domain.StoryCluster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryClusterRepository extends JpaRepository<StoryCluster, Long> {

	List<StoryCluster> findByTopicId(Long topicId);
}
