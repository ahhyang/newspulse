package com.newspulse.web;

import com.newspulse.dto.IngestionRunResponse;
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

	public IngestionController(IngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping("/runs")
	@SecurityRequirement(name = "bearer-jwt")
	@Operation(summary = "Trigger an on-demand ingestion run across all active topics (admin)")
	public ResponseEntity<IngestionRunResponse> run() {
		return ResponseEntity.ok(ingestionService.ingestAll());
	}
}
