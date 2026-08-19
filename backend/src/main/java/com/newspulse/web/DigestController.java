package com.newspulse.web;

import com.newspulse.dto.DigestResponse;
import com.newspulse.dto.DigestRunResponse;
import com.newspulse.service.DigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digests")
@Tag(name = "Digests")
public class DigestController {

	private final DigestService digestService;

	public DigestController(DigestService digestService) {
		this.digestService = digestService;
	}

	@GetMapping("/latest")
	@Operation(summary = "Get the most recently generated digest")
	public DigestResponse latest(@RequestParam(required = false) Long topicId) {
		return digestService.getLatest(topicId);
	}

	@PostMapping("/runs")
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Generate (or regenerate) digests for a UTC calendar date (admin)")
	public ResponseEntity<DigestRunResponse> generate(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Long topicId
	) {
		LocalDate digestDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
		if (topicId != null) {
			Instant started = Instant.now();
			digestService.generateOne(topicId, digestDate);
			return ResponseEntity.ok(new DigestRunResponse(started, Instant.now(), digestDate, 1, 0, 1));
		}
		return ResponseEntity.ok(digestService.generateAll(digestDate));
	}

	@GetMapping("/{date}")
	@Operation(summary = "Get the digest for a calendar date (ISO-8601, UTC)")
	public DigestResponse byDate(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Long topicId
	) {
		return digestService.getByDate(date, topicId);
	}
}
