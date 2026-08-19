package com.newspulse.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

	@Test
	void stripsSchemeTrackingParamsAndTrailingSlash() {
		String left = UrlNormalizer.normalize("https://WWW.Example.com/AI/Story/?utm_source=feed&b=2&a=1");
		String right = UrlNormalizer.normalize("http://www.example.com/AI/Story?a=1&b=2");
		assertThat(left).isEqualTo(right).isEqualTo("www.example.com/AI/Story?a=1&b=2");
	}

	@Test
	void emptyInputNormalizesToEmpty() {
		assertThat(UrlNormalizer.normalize("  ")).isEmpty();
		assertThat(UrlNormalizer.normalize(null)).isEmpty();
	}
}
