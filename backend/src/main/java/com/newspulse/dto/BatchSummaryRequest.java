package com.newspulse.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchSummaryRequest(
		@NotEmpty @Size(min = 2, max = 12) List<Long> articleIds
) {}
