package com.newspulse.dto;

import java.time.Instant;

public record EnrichmentRunResponse(
		Instant startedAt,
		Instant finishedAt,
		int scanned,
		int enriched,
		int skipped,
		int failures
) {}
