package com.newspulse.service;

import com.newspulse.clustering.StoryClusteringService;
import com.newspulse.config.AppProperties;
import com.newspulse.domain.Article;
import com.newspulse.domain.Digest;
import com.newspulse.domain.DigestItem;
import com.newspulse.domain.Sentiment;
import com.newspulse.domain.StoryCluster;
import com.newspulse.domain.Topic;
import com.newspulse.dto.DigestResponse;
import com.newspulse.dto.DigestRunResponse;
import com.newspulse.mapper.NewsPulseMapper;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.DigestRepository;
import com.newspulse.repository.TopicRepository;
import com.newspulse.web.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DigestService {

	private static final Logger log = LoggerFactory.getLogger(DigestService.class);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private final DigestRepository digestRepository;
	private final TopicRepository topicRepository;
	private final ArticleRepository articleRepository;
	private final StoryClusteringService clusteringService;
	private final AppProperties appProperties;
	private final TransactionTemplate transactionTemplate;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public DigestService(
			DigestRepository digestRepository,
			TopicRepository topicRepository,
			ArticleRepository articleRepository,
			StoryClusteringService clusteringService,
			AppProperties appProperties,
			PlatformTransactionManager transactionManager
	) {
		this.digestRepository = digestRepository;
		this.topicRepository = topicRepository;
		this.articleRepository = articleRepository;
		this.clusteringService = clusteringService;
		this.appProperties = appProperties;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Transactional(readOnly = true)
	public DigestResponse getLatest(Long topicId) {
		return (topicId == null
				? digestRepository.findFirstByOrderByDigestDateDescGeneratedAtDesc()
				: digestRepository.findFirstByTopicIdOrderByDigestDateDescGeneratedAtDesc(topicId))
				.map(NewsPulseMapper::toResponse)
				.orElseThrow(() -> new NotFoundException("No digest has been generated yet"));
	}

	@Transactional(readOnly = true)
	public DigestResponse getByDate(LocalDate date, Long topicId) {
		Long resolvedTopicId = resolveTopicId(topicId);
		return digestRepository.findByTopicIdAndDigestDate(resolvedTopicId, date)
				.map(NewsPulseMapper::toResponse)
				.orElseThrow(() -> new NotFoundException("No digest found for %s".formatted(date)));
	}

	@Async
	public void generateForTodayAsync() {
		try {
			generateAll(LocalDate.now(ZoneOffset.UTC));
		} catch (Exception ex) {
			log.error("Async digest generation failed without crashing the application", ex);
		}
	}

	public DigestRunResponse generateAll(LocalDate date) {
		if (!running.compareAndSet(false, true)) {
			log.info("Digest generation already in progress; skipping");
			return new DigestRunResponse(Instant.now(), Instant.now(), date, 0, 0, 0);
		}
		Instant started = Instant.now();
		int clustersUpdated = 0;
		int written = 0;
		List<Topic> topics = topicRepository.findAllByActiveTrueOrderByNameAsc();
		try {
			for (Topic topic : topics) {
				clustersUpdated += clusteringService.clusterTopic(topic);
				persistDigest(topic, date);
				written++;
			}
			return new DigestRunResponse(started, Instant.now(), date, topics.size(), clustersUpdated, written);
		} finally {
			running.set(false);
		}
	}

	public DigestResponse generateOne(Long topicId, LocalDate date) {
		Topic topic = topicRepository.findById(resolveTopicId(topicId))
				.orElseThrow(() -> new NotFoundException("Topic %d was not found".formatted(topicId)));
		clusteringService.clusterTopic(topic);
		return persistAndMap(topic, date);
	}

	private Digest persistDigest(Topic topic, LocalDate date) {
		Digest saved = transactionTemplate.execute(status -> persistInTransaction(topic, date));
		if (saved == null) {
			throw new IllegalStateException("Digest persist returned null");
		}
		return saved;
	}

	private DigestResponse persistAndMap(Topic topic, LocalDate date) {
		DigestResponse response = transactionTemplate.execute(status ->
				NewsPulseMapper.toResponse(persistInTransaction(topic, date)));
		if (response == null) {
			throw new IllegalStateException("Digest persist returned null");
		}
		return response;
	}

	private Digest persistInTransaction(Topic topic, LocalDate date) {
		Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		List<Article> articles = articleRepository
				.findByTopicIdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(topic.getId(), from, to)
				.stream()
				.filter(article -> article.getEnrichment() != null)
				.toList();

		SentimentTally tally = tally(articles);
		List<List<Article>> ranked = rankStories(articles);
		int limit = Math.max(1, appProperties.digest().maxItems());

		digestRepository.findByTopicIdAndDigestDate(topic.getId(), date).ifPresent(existing -> {
			digestRepository.delete(existing);
			digestRepository.flush();
		});

		Digest digest = new Digest();
		digest.setTopic(topic);
		digest.setDigestDate(date);
		digest.setPositivePct(tally.positivePct);
		digest.setNeutralPct(tally.neutralPct);
		digest.setNegativePct(tally.negativePct);
		digest.setHeadline(headline(topic, date, ranked));
		digest.setOverview(overview(ranked.size(), articles.size(), tally));

		int rank = 1;
		for (List<Article> group : ranked) {
			if (rank > limit) {
				break;
			}
			DigestItem item = toItem(digest, group, rank);
			digest.getItems().add(item);
			rank++;
		}
		return digestRepository.save(digest);
	}

	private List<List<Article>> rankStories(List<Article> articles) {
		Map<String, List<Article>> groups = new LinkedHashMap<>();
		for (Article article : articles) {
			String key = article.getCluster() != null ? "c-" + article.getCluster().getId() : "a-" + article.getId();
			groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(article);
		}
		return groups.values().stream()
				.sorted(Comparator
						.<List<Article>>comparingInt(DigestService::distinctSources)
						.reversed()
						.thenComparing(DigestService::latestPublished, Comparator.reverseOrder()))
				.toList();
	}

	private DigestItem toItem(Digest digest, List<Article> group, int rank) {
		Article lead = group.stream()
				.max(Comparator.comparing(DigestService::publishedOrEpoch))
				.orElse(group.getFirst());
		DigestItem item = new DigestItem();
		item.setDigest(digest);
		item.setRank(rank);
		item.setTitle(truncate(lead.getTitle(), 500));
		item.setSummary(lead.getEnrichment().getSummary());
		item.setSourceCount(distinctSources(group));
		item.setSentiment(majoritySentiment(group));
		StoryCluster cluster = lead.getCluster();
		item.setCluster(cluster);
		return item;
	}

	private Long resolveTopicId(Long topicId) {
		if (topicId != null) {
			return topicId;
		}
		List<Topic> active = topicRepository.findAllByActiveTrueOrderByNameAsc();
		if (active.isEmpty()) {
			throw new NotFoundException("No active topics are configured");
		}
		return active.getFirst().getId();
	}

	private static SentimentTally tally(List<Article> articles) {
		long positive = 0;
		long neutral = 0;
		long negative = 0;
		for (Article article : articles) {
			Sentiment sentiment = article.getEnrichment().getSentiment();
			if (sentiment == Sentiment.POSITIVE) {
				positive++;
			} else if (sentiment == Sentiment.NEGATIVE) {
				negative++;
			} else {
				neutral++;
			}
		}
		long total = positive + neutral + negative;
		return new SentimentTally(positive, neutral, negative, pct(positive, total), pct(neutral, total), pct(negative, total));
	}

	private static Sentiment majoritySentiment(List<Article> group) {
		long positive = group.stream().filter(a -> a.getEnrichment().getSentiment() == Sentiment.POSITIVE).count();
		long negative = group.stream().filter(a -> a.getEnrichment().getSentiment() == Sentiment.NEGATIVE).count();
		long neutral = group.size() - positive - negative;
		if (positive >= negative && positive >= neutral) {
			return Sentiment.POSITIVE;
		}
		if (negative >= positive && negative >= neutral) {
			return Sentiment.NEGATIVE;
		}
		return Sentiment.NEUTRAL;
	}

	private static String headline(Topic topic, LocalDate date, List<List<Article>> ranked) {
		if (ranked.isEmpty()) {
			return "%s briefing — %s".formatted(topic.getName(), date.format(DAY));
		}
		Article lead = ranked.getFirst().stream()
				.max(Comparator.comparing(DigestService::publishedOrEpoch))
				.orElse(ranked.getFirst().getFirst());
		return truncate("%s: %s".formatted(topic.getName(), lead.getTitle()), 500);
	}

	private static String overview(int storyCount, int articleCount, SentimentTally tally) {
		if (articleCount == 0) {
			return "No enriched articles were available for this day.";
		}
		return "%d distinct %s from %d article%s. Coverage: %s%% positive, %s%% neutral, %s%% negative.".formatted(
				storyCount,
				storyCount == 1 ? "story" : "stories",
				articleCount,
				articleCount == 1 ? "" : "s",
				tally.positivePct.toPlainString(),
				tally.neutralPct.toPlainString(),
				tally.negativePct.toPlainString()
		);
	}

	private static int distinctSources(List<Article> group) {
		return (int) group.stream().map(Article::getSourceName).filter(Objects::nonNull).distinct().count();
	}

	private static Instant latestPublished(List<Article> group) {
		return group.stream().map(DigestService::publishedOrEpoch).max(Instant::compareTo).orElse(Instant.EPOCH);
	}

	private static Instant publishedOrEpoch(Article article) {
		return article.getPublishedAt() == null ? Instant.EPOCH : article.getPublishedAt();
	}

	private static BigDecimal pct(long part, long total) {
		if (total == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
		}
		return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		String stripped = value.strip();
		return stripped.length() <= max ? stripped : stripped.substring(0, max);
	}

	private record SentimentTally(
			long positive,
			long neutral,
			long negative,
			BigDecimal positivePct,
			BigDecimal neutralPct,
			BigDecimal negativePct
	) {}
}
