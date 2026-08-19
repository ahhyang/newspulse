package com.newspulse.dto;

import jakarta.validation.constraints.Size;

public record UpdateTopicRequest(
		@Size(max = 120) String name,
		@Size(max = 255) String query,
		@Size(max = 500) String description,
		Boolean active
) {}
