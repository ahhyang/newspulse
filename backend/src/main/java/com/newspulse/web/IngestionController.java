package com.newspulse.web;

import com.newspulse.dto.IngestionRunResponse;
import com.newspulse.service.EnrichmentService;
import com.newspulse.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
@Tag(name = "Ingestion")
public class IngestionController {

	private final IngestionService ingestionService;
	private final EnrichmentService enrichmentService;

	public IngestionController(IngestionService ingestionService, EnrichmentService enrichmentService) {
		this.ingestionService = ingestionService;
		this.enrichmentService = enrichmentService;
	}

	@PostMapping("/runs")
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Trigger an on-demand ingestion run across all active topics (admin)")
	public ResponseEntity<IngestionRunResponse> run() {
		IngestionRunResponse result = ingestionService.ingestAll();
		if (result.inserted() > 0) {
			enrichmentService.enrichUnprocessedAsync();
		}
		return ResponseEntity.ok(result);
	}
}
