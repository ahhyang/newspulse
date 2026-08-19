package com.newspulse.clustering;

import com.newspulse.config.AppProperties;
import com.newspulse.domain.Article;
import com.newspulse.domain.StoryCluster;
import com.newspulse.domain.Topic;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.StoryClusterRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryClusteringService {

	private final ArticleRepository articleRepository;
	private final StoryClusterRepository clusterRepository;
	private final AppProperties appProperties;

	public StoryClusteringService(
			ArticleRepository articleRepository,
			StoryClusterRepository clusterRepository,
			AppProperties appProperties
	) {
		this.articleRepository = articleRepository;
		this.clusterRepository = clusterRepository;
		this.appProperties = appProperties;
	}

	@Transactional
	public int clusterTopic(Topic topic) {
		List<StoryCluster> existing = new ArrayList<>(clusterRepository.findByTopicId(topic.getId()));
		List<Article> unclustered = articleRepository.findByTopicIdAndClusterIsNull(topic.getId());
		if (unclustered.isEmpty()) {
			return 0;
		}

		int[] parent = new int[unclustered.size()];
		for (int i = 0; i < parent.length; i++) {
			parent[i] = i;
		}
		for (int i = 0; i < unclustered.size(); i++) {
			for (int j = i + 1; j < unclustered.size(); j++) {
				if (sameStory(unclustered.get(i), unclustered.get(j))) {
					union(parent, i, j);
				}
			}
		}

		Map<Integer, List<Article>> components = new HashMap<>();
		for (int i = 0; i < unclustered.size(); i++) {
			components.computeIfAbsent(find(parent, i), key -> new ArrayList<>()).add(unclustered.get(i));
		}

		int assigned = 0;
		for (List<Article> group : components.values()) {
			Article representative = representative(group);
			StoryCluster cluster = findMatchingCluster(existing, representative);
			if (cluster == null) {
				cluster = new StoryCluster();
				cluster.setTopic(topic);
				cluster.setCanonicalTitle(truncate(representative.getTitle(), 500));
				cluster.setCanonicalSummary(summaryOf(representative));
				cluster.setArticleCount(0);
				cluster = clusterRepository.save(cluster);
				existing.add(cluster);
			}
			for (Article article : group) {
				article.setCluster(cluster);
				articleRepository.save(article);
				assigned++;
			}
			cluster.setArticleCount(cluster.getArticleCount() + group.size());
			if (isBetterCanonical(representative, cluster)) {
				cluster.setCanonicalTitle(truncate(representative.getTitle(), 500));
				cluster.setCanonicalSummary(summaryOf(representative));
			}
		}
		return assigned;
	}

	private boolean sameStory(Article left, Article right) {
		if (left.getContentHash() != null
				&& !left.getContentHash().isBlank()
				&& left.getContentHash().equals(right.getContentHash())) {
			return true;
		}
		return TitleSimilarity.jaccard(left.getTitle(), right.getTitle()) >= appProperties.digest().clusterSimilarity();
	}

	private StoryCluster findMatchingCluster(List<StoryCluster> existing, Article article) {
		StoryCluster best = null;
		double bestScore = appProperties.digest().clusterSimilarity();
		for (StoryCluster cluster : existing) {
			if (TitleSimilarity.jaccard(article.getTitle(), cluster.getCanonicalTitle()) >= bestScore) {
				best = cluster;
				bestScore = TitleSimilarity.jaccard(article.getTitle(), cluster.getCanonicalTitle());
			}
		}
		return best;
	}

	private static boolean isBetterCanonical(Article article, StoryCluster cluster) {
		String next = summaryOf(article);
		String current = cluster.getCanonicalSummary();
		return current == null || current.isBlank() || (next != null && next.length() > current.length());
	}

	private static String summaryOf(Article article) {
		if (article.getEnrichment() != null && article.getEnrichment().getSummary() != null) {
			return article.getEnrichment().getSummary();
		}
		return article.getRawContent();
	}

	private static Article representative(List<Article> group) {
		return group.stream()
				.min((a, b) -> {
					var left = a.getPublishedAt();
					var right = b.getPublishedAt();
					if (left == null && right == null) {
						return Long.compare(a.getId() == null ? 0 : a.getId(), b.getId() == null ? 0 : b.getId());
					}
					if (left == null) {
						return 1;
					}
					if (right == null) {
						return -1;
					}
					return right.compareTo(left);
				})
				.orElse(group.getFirst());
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		String stripped = value.strip();
		return stripped.length() <= max ? stripped : stripped.substring(0, max);
	}

	private static int find(int[] parent, int i) {
		if (parent[i] != i) {
			parent[i] = find(parent, parent[i]);
		}
		return parent[i];
	}

	private static void union(int[] parent, int i, int j) {
		int a = find(parent, i);
		int b = find(parent, j);
		if (a != b) {
			parent[b] = a;
		}
	}
}
