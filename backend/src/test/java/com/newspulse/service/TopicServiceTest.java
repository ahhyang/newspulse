package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newspulse.domain.Topic;
import com.newspulse.dto.CreateTopicRequest;
import com.newspulse.dto.TopicResponse;
import com.newspulse.dto.UpdateTopicRequest;
import com.newspulse.repository.TopicRepository;
import com.newspulse.web.ConflictException;
import com.newspulse.web.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

	@Mock
	private TopicRepository topicRepository;

	private TopicService topicService;

	@BeforeEach
	void setUp() {
		topicService = new TopicService(topicRepository);
	}

	@Test
	void createPersistsTrimmedTopicWhenNameIsUnique() {
		when(topicRepository.existsByNameIgnoreCase("AI Industry")).thenReturn(false);
		when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
			Topic topic = invocation.getArgument(0);
			topic.setId(1L);
			topic.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
			topic.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
			return topic;
		});

		TopicResponse response = topicService.create(new CreateTopicRequest(
				"  AI Industry  ",
				"  generative AI  ",
				"Labs and models"
		));

		ArgumentCaptor<Topic> captor = ArgumentCaptor.forClass(Topic.class);
		verify(topicRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("AI Industry");
		assertThat(captor.getValue().getQuery()).isEqualTo("generative AI");
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.active()).isTrue();
	}

	@Test
	void createRejectsDuplicateName() {
		when(topicRepository.existsByNameIgnoreCase("AI Industry")).thenReturn(true);

		assertThatThrownBy(() -> topicService.create(new CreateTopicRequest("AI Industry", "ai", null)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	void getThrowsWhenMissing() {
		when(topicRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> topicService.get(99L))
				.isInstanceOf(NotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void updateCanDeactivateTopic() {
		Topic existing = new Topic("AI Industry", "ai", "desc");
		existing.setId(1L);
		existing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		existing.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		when(topicRepository.findById(1L)).thenReturn(Optional.of(existing));

		TopicResponse updated = topicService.update(1L, new UpdateTopicRequest(null, null, null, false));

		assertThat(updated.active()).isFalse();
	}

	@Test
	void listMapsAllTopics() {
		Topic topic = new Topic("AI Industry", "ai", "desc");
		topic.setId(1L);
		topic.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		topic.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		when(topicRepository.findAll()).thenReturn(List.of(topic));

		assertThat(topicService.list()).hasSize(1).first().extracting(TopicResponse::name).isEqualTo("AI Industry");
	}
}
