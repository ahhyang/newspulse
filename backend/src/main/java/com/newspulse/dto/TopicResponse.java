package com.newspulse.dto;

import java.time.Instant;

public record TopicResponse(
		Long id,
		String name,
		String query,
		String description,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {}
