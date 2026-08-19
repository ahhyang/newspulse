package com.newspulse.service;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.DigestRunResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DigestScheduler {

	private static final Logger log = LoggerFactory.getLogger(DigestScheduler.class);

	private final DigestService digestService;
	private final AppProperties appProperties;

	public DigestScheduler(DigestService digestService, AppProperties appProperties) {
		this.digestService = digestService;
		this.appProperties = appProperties;
	}

	@Scheduled(cron = "${app.digest.cron:0 5 0 * * *}", zone = "UTC")
	public void scheduledDigest() {
		if (!appProperties.digest().enabled()) {
			return;
		}
		LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
		try {
			DigestRunResponse result = digestService.generateAll(yesterday);
			log.info(
					"Scheduled digest finished: date={} topics={} clusters={} written={}",
					result.digestDate(),
					result.topics(),
					result.clustersUpdated(),
					result.digestsWritten()
			);
		} catch (Exception ex) {
			log.error("Scheduled digest failed without crashing the application", ex);
		}
	}
}
