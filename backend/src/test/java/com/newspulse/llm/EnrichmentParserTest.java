package com.newspulse.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newspulse.domain.Sentiment;
import org.junit.jupiter.api.Test;

class EnrichmentParserTest {

	@Test
	void extractsJsonFromMarkdownFence() {
		String raw = """
				```json
				{"summary":"ok","sentiment":"POSITIVE"}
				```
				""";
		assertThat(EnrichmentParser.extractJsonObject(raw)).contains("\"summary\":\"ok\"");
	}

	@Test
	void parseSentimentAcceptsPrefixes() {
		assertThat(EnrichmentParser.parseSentiment("positive")).isEqualTo(Sentiment.POSITIVE);
		assertThat(EnrichmentParser.parseSentiment("Neg")).isEqualTo(Sentiment.NEGATIVE);
		assertThat(EnrichmentParser.parseSentiment("mixed")).isEqualTo(Sentiment.NEUTRAL);
	}

	@Test
	void normalizeStanceMapsUnknownLongValuesToOther() {
		assertThat(EnrichmentParser.normalizeStance("Regulatory")).isEqualTo("regulatory");
		assertThat(EnrichmentParser.normalizeStance("this is a rambling stance label")).isEqualTo("other");
	}

	@Test
	void emptyContentIsRejected() {
		assertThatThrownBy(() -> EnrichmentParser.extractJsonObject("   "))
				.isInstanceOf(LlmException.class);
	}
}
