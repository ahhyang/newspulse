package com.newspulse.dto;

import java.time.Instant;

public record IngestionRunResponse(
		Instant startedAt,
		Instant finishedAt,
		int topicsScanned,
		int fetched,
		int inserted,
		int duplicates,
		int skippedInvalid,
		int sourceFailures
) {}
