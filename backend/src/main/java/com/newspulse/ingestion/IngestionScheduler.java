package com.newspulse.ingestion;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.IngestionRunResponse;
import com.newspulse.service.IngestionService;
import com.newspulse.web.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IngestionScheduler {

	private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

	private final IngestionService ingestionService;
	private final AppProperties appProperties;

	public IngestionScheduler(IngestionService ingestionService, AppProperties appProperties) {
		this.ingestionService = ingestionService;
		this.appProperties = appProperties;
	}

	@Scheduled(
			fixedDelayString = "${app.ingestion.interval-ms}",
			initialDelayString = "${app.ingestion.initial-delay-ms:20000}"
	)
	public void scheduledIngest() {
		if (!appProperties.ingestion().enabled()) {
			return;
		}
		try {
			IngestionRunResponse result = ingestionService.ingestAll();
			log.info(
					"Scheduled ingestion finished: topics={} fetched={} inserted={} duplicates={} failures={}",
					result.topicsScanned(),
					result.fetched(),
					result.inserted(),
					result.duplicates(),
					result.sourceFailures()
			);
		} catch (ConflictException ex) {
			log.info("Skipping scheduled ingestion: {}", ex.getMessage());
		} catch (Exception ex) {
			log.error("Scheduled ingestion failed without crashing the application", ex);
		}
	}
}
