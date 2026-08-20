package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.newspulse.domain.Article;
import com.newspulse.domain.ArticleEnrichment;
import com.newspulse.domain.Sentiment;
import com.newspulse.domain.Topic;
import com.newspulse.dto.BatchSummaryRequest;
import com.newspulse.llm.LlmClient;
import com.newspulse.repository.ArticleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchSummaryServiceTest {

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private LlmClient llmClient;

	private BatchSummaryService service;

	@BeforeEach
	void setUp() {
		service = new BatchSummaryService(articleRepository, llmClient);
	}

	@Test
	void summarizesSelectedArticles() {
		Article one = article(1L, "OpenAI ships tool use");
		Article two = article(2L, "Samsung raises chip prices");
		when(articleRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(one, two));
		when(llmClient.summarizeBatch(anyList())).thenReturn(new LlmClient.BatchSummaryResult(
				"Hardware and product news",
				"Labs and chipmakers drove the narrative.",
				List.of("product", "hardware"),
				"stub-model"
		));

		var response = service.summarize(new BatchSummaryRequest(List.of(1L, 2L)));

		assertThat(response.articleCount()).isEqualTo(2);
		assertThat(response.headline()).contains("Hardware");
		assertThat(response.articles()).extracting("title").containsExactly("OpenAI ships tool use", "Samsung raises chip prices");
	}

	private static Article article(Long id, String title) {
		Topic topic = new Topic("AI Industry", "ai", "desc");
		Article article = new Article();
		article.setId(id);
		article.setTopic(topic);
		article.setTitle(title);
		article.setUrl("https://example.com/" + id);
		article.setUrlHash("hash-" + id);
		article.setSource("gnews");
		article.setSourceName("Example News");
		ArticleEnrichment enrichment = new ArticleEnrichment();
		enrichment.setSummary("Short summary.");
		enrichment.setSentiment(Sentiment.NEUTRAL);
		enrichment.setArticle(article);
		article.setEnrichment(enrichment);
		return article;
	}
}
