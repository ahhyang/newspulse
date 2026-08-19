package com.newspulse.web;

import com.newspulse.dto.EnrichmentRunResponse;
import com.newspulse.service.EnrichmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrichment")
@Tag(name = "Enrichment")
public class EnrichmentController {

	private final EnrichmentService enrichmentService;

	public EnrichmentController(EnrichmentService enrichmentService) {
		this.enrichmentService = enrichmentService;
	}

	@PostMapping("/runs")
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Enrich a batch of unprocessed articles via the LLM (admin)")
	public ResponseEntity<EnrichmentRunResponse> run() {
		return ResponseEntity.ok(enrichmentService.enrichUnprocessed());
	}
}
