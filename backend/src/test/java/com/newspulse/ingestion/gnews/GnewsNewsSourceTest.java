package com.newspulse.ingestion.gnews;

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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GnewsNewsSourceTest {

	private static final String JSON = """
			{
			  "totalArticles": 1,
			  "articles": [
			    {
			      "title": "Lab releases new model",
			      "description": "A short blurb",
			      "content": "Full-ish content from GNews.",
			      "url": "https://news.example.com/ai/model",
			      "publishedAt": "2026-08-18T10:00:00Z",
			      "source": { "name": "Example News", "url": "https://news.example.com" }
			    }
			  ]
			}
			""";

	private MockRestServiceServer server;
	private GnewsNewsSource source;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("test-jwt-secret-key-that-is-long-enough-for-hs256-algorithms", 3600000),
				new AppProperties.Admin("admin", "s3cret"),
				new AppProperties.Cors("http://localhost:5173"),
				new AppProperties.Ingestion(false, 3600000, 20000, 24),
				new AppProperties.Llm("openrouter", "https://openrouter.ai/api/v1", "test", "model", "https://example.com", "NewsPulse"),
				new AppProperties.Gnews(true, "gnews-key", "https://gnews.io/api/v4", "en", 10)
		);
		source = new GnewsNewsSource(builder, properties);
	}

	@Test
	void mapsArticlesFromSearchPayload() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

		Topic topic = new Topic("AI Industry", "artificial intelligence", "desc");
		List<RawArticle> articles = source.fetch(topic, null);

		assertThat(articles).hasSize(1);
		RawArticle article = articles.getFirst();
		assertThat(article.title()).isEqualTo("Lab releases new model");
		assertThat(article.url()).isEqualTo("https://news.example.com/ai/model");
		assertThat(article.sourceId()).isEqualTo("gnews");
		assertThat(article.sourceName()).isEqualTo("Example News");
		assertThat(article.rawContent()).isEqualTo("Full-ish content from GNews.");
		assertThat(article.publishedAt()).isEqualTo(Instant.parse("2026-08-18T10:00:00Z"));
		server.verify();
	}

	@Test
	void nonRetryableErrorBecomesIngestionSourceException() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
				.andRespond(withRawStatus(401).body("{\"errors\":[\"Your API key is invalid.\"]}").contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> source.fetch(new Topic("AI Industry", "ai", null), null))
				.isInstanceOf(IngestionSourceException.class)
				.hasMessageContaining("401");
	}
}
