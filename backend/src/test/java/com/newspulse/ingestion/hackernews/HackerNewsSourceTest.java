package com.newspulse.ingestion.hackernews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.newspulse.config.AppProperties;
import com.newspulse.domain.Topic;
import com.newspulse.ingestion.IngestionSourceException;
import com.newspulse.ingestion.NewsSource.RawArticle;
import com.newspulse.support.AppPropertiesFixture;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HackerNewsSourceTest {

	private static final String JSON = """
			{
			  "hits": [
			    {
			      "title": "Show HN: NewsPulse briefing",
			      "url": "https://example.com/newspulse",
			      "objectID": "111",
			      "created_at": "2026-08-18T10:00:00.000Z",
			      "story_text": "A launch post.",
			      "author": "alice"
			    },
			    {
			      "title": "Ask HN: How do you follow AI news?",
			      "url": null,
			      "objectID": "222",
			      "created_at": "2026-08-18T11:00:00.000Z",
			      "story_text": null,
			      "author": "bob"
			    }
			  ]
			}
			""";

	private MockRestServiceServer server;
	private HackerNewsSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		AppProperties properties = AppPropertiesFixture.defaults();
		source = new HackerNewsSource(builder, properties);
	}

	@Test
	void mapsHitsAndFallsBackToHnItemUrl() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/search_by_date")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

		Topic topic = new Topic("AI Industry", "artificial intelligence", "desc");
		List<RawArticle> articles = source.fetch(topic, null);

		assertThat(articles).hasSize(2);
		RawArticle linked = articles.getFirst();
		assertThat(linked.title()).isEqualTo("Show HN: NewsPulse briefing");
		assertThat(linked.url()).isEqualTo("https://example.com/newspulse");
		assertThat(linked.sourceId()).isEqualTo("hackernews");
		assertThat(linked.sourceName()).isEqualTo("Hacker News");
		assertThat(linked.rawContent()).isEqualTo("A launch post.");
		assertThat(linked.publishedAt()).isEqualTo(Instant.parse("2026-08-18T10:00:00.000Z"));

		RawArticle ask = articles.get(1);
		assertThat(ask.url()).isEqualTo("https://news.ycombinator.com/item?id=222");
		assertThat(ask.rawContent()).isEqualTo("Ask HN: How do you follow AI news?");
		server.verify();
	}

	@Test
	void nonRetryableErrorBecomesIngestionSourceException() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/search_by_date")))
				.andRespond(withRawStatus(400).body("{\"message\":\"bad query\"}").contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> source.fetch(new Topic("AI Industry", "ai", null), null))
				.isInstanceOf(IngestionSourceException.class)
				.hasMessageContaining("400");
	}
}
