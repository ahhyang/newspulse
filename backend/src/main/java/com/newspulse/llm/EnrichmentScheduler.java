package com.newspulse.llm;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.EnrichmentRunResponse;
import com.newspulse.service.EnrichmentService;
import com.newspulse.web.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EnrichmentScheduler {

	private static final Logger log = LoggerFactory.getLogger(EnrichmentScheduler.class);

	private final EnrichmentService enrichmentService;
	private final AppProperties appProperties;

	public EnrichmentScheduler(EnrichmentService enrichmentService, AppProperties appProperties) {
		this.enrichmentService = enrichmentService;
		this.appProperties = appProperties;
	}

	@Scheduled(
			fixedDelayString = "${app.enrichment.interval-ms}",
			initialDelayString = "${app.enrichment.initial-delay-ms:40000}"
	)
	public void scheduledEnrich() {
		if (!appProperties.enrichment().enabled()) {
			return;
		}
		try {
			EnrichmentRunResponse result = enrichmentService.enrichUnprocessed();
			if (result.scanned() == 0) {
				return;
			}
			log.info(
					"Scheduled enrichment finished: scanned={} enriched={} skipped={} failures={}",
					result.scanned(),
					result.enriched(),
					result.skipped(),
					result.failures()
			);
		} catch (ConflictException ex) {
			log.info("Skipping scheduled enrichment: {}", ex.getMessage());
		} catch (Exception ex) {
			log.error("Scheduled enrichment failed without crashing the application", ex);
		}
	}
}
