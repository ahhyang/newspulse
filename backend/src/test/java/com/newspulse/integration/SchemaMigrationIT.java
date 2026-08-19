package com.newspulse.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.newspulse.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SchemaMigrationIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private TopicRepository topicRepository;

	@Test
	void flywaySeedsDefaultAiIndustryTopic() {
		assertThat(topicRepository.findByNameIgnoreCase("AI Industry")).isPresent();
		assertThat(topicRepository.count()).isGreaterThanOrEqualTo(1);
	}
}
