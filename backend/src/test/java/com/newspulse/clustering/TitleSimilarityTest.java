package com.newspulse.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class TitleSimilarityTest {

	@Test
	void similarHeadlinesScoreAboveClusterThreshold() {
		double score = TitleSimilarity.jaccard(
				"OpenAI releases GPT-5 model to API",
				"OpenAI releases GPT-5 to the API"
		);
		assertThat(score).isGreaterThan(0.45);
	}

	@Test
	void unrelatedHeadlinesScoreLow() {
		double score = TitleSimilarity.jaccard(
				"OpenAI releases GPT-5 model to API",
				"Farmers protest new dairy tariffs in France"
		);
		assertThat(score).isLessThan(0.2);
	}

	@Test
	void emptyTitlesAreNotSimilar() {
		assertThat(TitleSimilarity.jaccard("", "anything")).isZero();
		assertThat(TitleSimilarity.jaccard(null, "title")).isZero();
	}

	@Test
	void nearDuplicatePhrasingIsDetected() {
		assertThat(TitleSimilarity.jaccard(
				"Regulators propose AI labeling rules",
				"Regulators propose new AI labeling rules"
		)).isCloseTo(1.0, within(0.2));
	}
}
