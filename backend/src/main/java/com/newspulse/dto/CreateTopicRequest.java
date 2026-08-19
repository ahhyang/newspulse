package com.newspulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 255) String query,
		@Size(max = 500) String description
) {}
