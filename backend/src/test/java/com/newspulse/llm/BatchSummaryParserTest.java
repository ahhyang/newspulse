package com.newspulse.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BatchSummaryParserTest {

	@Test
	void parsesBatchSummaryJson() {
		String json = """
				{
				  "headline": "AI labs and chips dominate the week",
				  "overview": "Coverage focused on model launches and supply constraints.",
				  "themes": ["models", "hardware"]
				}
				""";
		LlmClient.BatchSummaryResult result = BatchSummaryParser.parse(json, "test-model");
		assertThat(result.headline()).contains("AI labs");
		assertThat(result.overview()).contains("Coverage");
		assertThat(result.themes()).containsExactly("models", "hardware");
		assertThat(result.model()).isEqualTo("test-model");
	}

	@Test
	void rejectsMissingOverview() {
		assertThatThrownBy(() -> BatchSummaryParser.parse("{\"headline\":\"Only headline\"}", "m"))
				.isInstanceOf(LlmException.class);
	}
}
