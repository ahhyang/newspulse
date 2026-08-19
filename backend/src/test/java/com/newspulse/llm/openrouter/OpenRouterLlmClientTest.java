package com.newspulse.llm.openrouter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newspulse.domain.Sentiment;
import com.newspulse.llm.LlmClient.EnrichmentResult;
import com.newspulse.llm.LlmException;
import com.newspulse.support.AppPropertiesFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterLlmClientTest {

	private static final String OPENROUTER_BODY = """
			{
			  "model": "anthropic/claude-3.5-sonnet",
			  "choices": [
			    {
			      "message": {
			        "role": "assistant",
			        "content": "{\\"summary\\":\\"Labs shipped a smaller model.\\",\\"sentiment\\":\\"POSITIVE\\",\\"justification\\":\\"Coverage is upbeat about capability.\\",\\"stance\\":\\"technical\\"}"
			      }
			    }
			  ]
			}
			""";

	private MockRestServiceServer server;
	private OpenRouterLlmClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new OpenRouterLlmClient(builder, AppPropertiesFixture.defaults("or-key"), new ObjectMapper());
	}

	@Test
	void mapsChatCompletionJsonToEnrichment() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(OPENROUTER_BODY, MediaType.APPLICATION_JSON));

		EnrichmentResult result = client.enrich("New model", "A lab released weights.");

		assertThat(result.summary()).contains("smaller model");
		assertThat(result.sentiment()).isEqualTo(Sentiment.POSITIVE);
		assertThat(result.justification()).contains("upbeat");
		assertThat(result.stanceTag()).isEqualTo("technical");
		assertThat(result.model()).isEqualTo("anthropic/claude-3.5-sonnet");
		server.verify();
	}

	@Test
	void nonRetryableErrorSurfacesAsLlmException() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
				.andRespond(withRawStatus(401).body("{\"error\":\"invalid key\"}").contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.enrich("Title", "Body"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("401");
	}
}
