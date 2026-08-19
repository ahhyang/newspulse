package com.newspulse.web;

import com.newspulse.dto.DigestResponse;
import com.newspulse.service.DigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping("/{date}")
	@Operation(summary = "Get the digest for a calendar date (ISO-8601)")
	public DigestResponse byDate(
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Long topicId
	) {
		return digestService.getByDate(date, topicId);
	}
}
