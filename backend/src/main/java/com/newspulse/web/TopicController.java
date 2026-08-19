package com.newspulse.web;

import com.newspulse.dto.CreateTopicRequest;
import com.newspulse.dto.TopicResponse;
import com.newspulse.dto.UpdateTopicRequest;
import com.newspulse.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
@Tag(name = "Topics")
public class TopicController {

	private final TopicService topicService;

	public TopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	@GetMapping
	@Operation(summary = "List tracked topics")
	public List<TopicResponse> list() {
		return topicService.list();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a topic by id")
	public TopicResponse get(@PathVariable Long id) {
		return topicService.get(id);
	}

	@PostMapping
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Create a tracked topic (admin)")
	public ResponseEntity<TopicResponse> create(@Valid @RequestBody CreateTopicRequest request) {
		TopicResponse created = topicService.create(request);
		return ResponseEntity.created(URI.create("/api/topics/" + created.id())).body(created);
	}

	@PatchMapping("/{id}")
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Update a tracked topic (admin)")
	public TopicResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTopicRequest request) {
		return topicService.update(id, request);
	}
}
