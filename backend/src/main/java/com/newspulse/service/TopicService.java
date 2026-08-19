package com.newspulse.service;

import com.newspulse.domain.Topic;
import com.newspulse.dto.CreateTopicRequest;
import com.newspulse.dto.TopicResponse;
import com.newspulse.dto.UpdateTopicRequest;
import com.newspulse.mapper.NewsPulseMapper;
import com.newspulse.repository.TopicRepository;
import com.newspulse.web.ConflictException;
import com.newspulse.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TopicService {

	private final TopicRepository topicRepository;

	public TopicService(TopicRepository topicRepository) {
		this.topicRepository = topicRepository;
	}

	@Transactional(readOnly = true)
	public List<TopicResponse> list() {
		return topicRepository.findAll().stream().map(NewsPulseMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public TopicResponse get(Long id) {
		return NewsPulseMapper.toResponse(requireTopic(id));
	}

	public TopicResponse create(CreateTopicRequest request) {
		String name = request.name().trim();
		String query = request.query().trim();
		if (topicRepository.existsByNameIgnoreCase(name)) {
			throw new ConflictException("A topic named '%s' already exists".formatted(name));
		}
		Topic topic = new Topic(name, query, request.description());
		return NewsPulseMapper.toResponse(topicRepository.save(topic));
	}

	public TopicResponse update(Long id, UpdateTopicRequest request) {
		Topic topic = requireTopic(id);
		if (request.name() != null && !request.name().isBlank()
				&& !request.name().equalsIgnoreCase(topic.getName())
				&& topicRepository.existsByNameIgnoreCase(request.name())) {
			throw new ConflictException("A topic named '%s' already exists".formatted(request.name()));
		}
		if (request.name() != null && !request.name().isBlank()) {
			topic.setName(request.name().trim());
		}
		if (request.query() != null && !request.query().isBlank()) {
			topic.setQuery(request.query().trim());
		}
		if (request.description() != null) {
			topic.setDescription(request.description());
		}
		if (request.active() != null) {
			topic.setActive(request.active());
		}
		return NewsPulseMapper.toResponse(topic);
	}

	private Topic requireTopic(Long id) {
		return topicRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Topic %d was not found".formatted(id)));
	}
}
