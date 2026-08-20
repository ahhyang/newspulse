package com.newspulse.web;

import com.newspulse.dto.BatchSummaryRequest;
import com.newspulse.dto.BatchSummaryResponse;
import com.newspulse.service.BatchSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summaries")
@Tag(name = "Summaries")
public class BatchSummaryController {

	private final BatchSummaryService batchSummaryService;

	public BatchSummaryController(BatchSummaryService batchSummaryService) {
		this.batchSummaryService = batchSummaryService;
	}

	@PostMapping("/batch")
	@Operation(summary = "Compose an AI briefing from a user-selected set of articles (2–12)")
	public ResponseEntity<BatchSummaryResponse> summarizeBatch(@Valid @RequestBody BatchSummaryRequest request) {
		return ResponseEntity.ok(batchSummaryService.summarize(request));
	}
}
