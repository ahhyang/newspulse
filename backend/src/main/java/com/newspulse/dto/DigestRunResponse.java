package com.newspulse.dto;

import java.time.Instant;
import java.time.LocalDate;

public record DigestRunResponse(
		Instant startedAt,
		Instant finishedAt,
		LocalDate digestDate,
		int topics,
		int clustersUpdated,
		int digestsWritten
) {}
