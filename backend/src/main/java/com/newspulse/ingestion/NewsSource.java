package com.newspulse.ingestion;

import com.newspulse.domain.Topic;
import java.time.Instant;
import java.util.List;

/**
 * Pluggable news source. Adding a provider must not change enrichment or digest logic.
 */
public interface NewsSource {

	String id();

	List<RawArticle> fetch(Topic topic, Instant since);

	record RawArticle(
			String title,
			String url,
			String sourceId,
			String sourceName,
			Instant publishedAt,
			String rawContent
	) {}
}
