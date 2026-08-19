package com.newspulse.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldViolation> details
) {
	public record FieldViolation(String field, String message) {}
}
