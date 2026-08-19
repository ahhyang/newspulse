package com.newspulse.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newspulse.domain.Article;
import com.newspulse.domain.StoryCluster;
import com.newspulse.domain.Topic;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.StoryClusterRepository;
import com.newspulse.support.AppPropertiesFixture;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoryClusteringServiceTest {

	@Mock
	private ArticleRepository articleRepository;

	@Mock
	private StoryClusterRepository clusterRepository;

	private StoryClusteringService clusteringService;
	private Topic topic;
	private final List<StoryCluster> savedClusters = new ArrayList<>();

	@BeforeEach
	void setUp() {
		topic = new Topic("AI Industry", "ai", "desc");
		topic.setId(1L);
		clusteringService = new StoryClusteringService(
				articleRepository,
				clusterRepository,
				AppPropertiesFixture.defaults()
		);
		when(clusterRepository.findByTopicId(1L)).thenReturn(savedClusters);
		when(clusterRepository.save(any(StoryCluster.class))).thenAnswer(invocation -> {
			StoryCluster cluster = invocation.getArgument(0);
			cluster.setId((long) (savedClusters.size() + 1));
			savedClusters.add(cluster);
			return cluster;
		});
		when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void groupsNearDuplicateTitlesIntoOneCluster() {
		Article first = article(10L, "OpenAI releases GPT-5 model to API");
		Article second = article(11L, "OpenAI releases GPT-5 to the API");
		Article other = article(12L, "Farmers protest dairy tariffs in France");
		when(articleRepository.findByTopicIdAndClusterIsNull(1L)).thenReturn(List.of(first, second, other));

		int assigned = clusteringService.clusterTopic(topic);

		assertThat(assigned).isEqualTo(3);
		assertThat(first.getCluster()).isNotNull();
		assertThat(first.getCluster()).isSameAs(second.getCluster());
		assertThat(other.getCluster()).isNotNull();
		assertThat(other.getCluster()).isNotSameAs(first.getCluster());
		assertThat(savedClusters).hasSize(2);
	}

	@Test
	void sameContentHashForcesSameCluster() {
		Article first = article(10L, "Completely different headline A");
		Article second = article(11L, "Unrelated wording B");
		first.setContentHash("abc");
		second.setContentHash("abc");
		when(articleRepository.findByTopicIdAndClusterIsNull(1L)).thenReturn(List.of(first, second));

		clusteringService.clusterTopic(topic);

		assertThat(first.getCluster()).isSameAs(second.getCluster());
		assertThat(savedClusters).hasSize(1);
	}

	private Article article(long id, String title) {
		Article article = new Article();
		article.setId(id);
		article.setTopic(topic);
		article.setTitle(title);
		article.setUrl("https://example.com/" + id);
		article.setUrlHash("hash-" + id);
		article.setSource("gnews");
		article.setSourceName("Example");
		article.setPublishedAt(Instant.parse("2026-08-18T10:00:00Z"));
		article.setRawContent("body");
		return article;
	}
}
