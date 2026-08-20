import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArticleCard } from "../components/ArticleCard";
import { EmptyState } from "../components/EmptyState";
import { StatCard } from "../components/StatCard";
import { useAutoRefresh } from "../hooks/useAutoRefresh";
import { ApiRequestError, api } from "../lib/api";
import { formatDay, formatInstant, formatPct, utcToday } from "../lib/format";
import type { Article, Digest, Stats } from "../lib/types";

const SentimentChart = lazy(() => import("../components/SentimentChart"));

export function OverviewPage() {
	const [params] = useSearchParams();
	const topicId = params.get("topicId") ? Number(params.get("topicId")) : undefined;
	const { tick, refreshing, refresh } = useAutoRefresh(180_000);
	const [digest, setDigest] = useState<Digest | null>(null);
	const [stats, setStats] = useState<Stats | null>(null);
	const [recent, setRecent] = useState<Article[]>([]);
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setError(null);
		void Promise.allSettled([
			api.digestLatest(topicId),
			api.stats(topicId),
			api.articles({ topicId, page: 0, size: 8 }),
		])
			.then(([digestResult, statsResult, articlesResult]) => {
				if (cancelled) {
					return;
				}
				if (statsResult.status === "fulfilled") {
					setStats(statsResult.value);
				}
				if (articlesResult.status === "fulfilled") {
					setRecent(articlesResult.value.content);
				}
				if (digestResult.status === "fulfilled") {
					setDigest(digestResult.value);
					return;
				}
				setDigest(null);
				const reason = digestResult.reason;
				if (!(reason instanceof ApiRequestError && reason.status === 404)) {
					setError(reason instanceof Error ? reason.message : "Could not load briefing");
				}
			})
			.finally(() => {
				if (!cancelled) {
					setLoading(false);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [topicId, tick]);

	const latestPoint = stats?.series.at(-1);
	const sentimentHint = latestPoint
		? `${formatPct(latestPoint.positivePct)} positive today`
		: "7-day enriched coverage";

	const sourceMix = useMemo(() => {
		const counts = recent.reduce<Record<string, number>>((acc, article) => {
			acc[article.source] = (acc[article.source] ?? 0) + 1;
			return acc;
		}, {});
		return Object.entries(counts)
			.map(([source, count]) => `${source === "hackernews" ? "HN" : source}: ${count}`)
			.join(" · ");
	}, [recent]);

	if (loading && !digest && recent.length === 0) {
		return <p className="text-sm text-mute">Loading your news pulse…</p>;
	}

	return (
		<div className="space-y-8">
			<section className="flex flex-wrap items-start justify-between gap-4">
				<div>
					<p className="text-xs uppercase tracking-[0.2em] text-gold">News pulse</p>
					<h1 className="mt-2 font-serif text-3xl leading-tight">Your AI industry radar</h1>
					<p className="mt-2 max-w-2xl text-sm leading-6 text-mute">
						GNews + Hacker News are fetched hourly, summarized every few minutes, and rolled into a daily
						briefing. This page refreshes automatically.
					</p>
				</div>
				<button
					type="button"
					onClick={refresh}
					className="rounded-md border border-line px-3 py-2 text-sm text-mute hover:border-gold/40 hover:text-paper"
				>
					{refreshing ? "Refreshing…" : "Refresh now"}
				</button>
			</section>

			<section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
				<StatCard label="Enriched articles" value={stats?.articleCount ?? "—"} hint={sentimentHint} accent="gold" />
				<StatCard
					label="Positive mix"
					value={digest ? formatPct(digest.positivePct) : latestPoint ? formatPct(latestPoint.positivePct) : "—"}
					hint="Latest briefing or today"
					accent="pos"
				/>
				<StatCard
					label="Neutral mix"
					value={digest ? formatPct(digest.neutralPct) : latestPoint ? formatPct(latestPoint.neutralPct) : "—"}
					accent="neu"
				/>
				<StatCard
					label="Negative mix"
					value={digest ? formatPct(digest.negativePct) : latestPoint ? formatPct(latestPoint.negativePct) : "—"}
					accent="neg"
				/>
			</section>

			{error ? <EmptyState title="Briefing unavailable" body={error} /> : null}

			<section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
				<div className="rounded-2xl border border-line bg-panel p-6">
					<div className="flex items-baseline justify-between gap-3">
						<h2 className="font-serif text-xl">Latest briefing</h2>
						<Link className="text-sm text-gold hover:underline" to={{ pathname: "/digest", search: params.toString() }}>
							Open full digest →
						</Link>
					</div>
					{digest ? (
						<div className="mt-4 space-y-3">
							<p className="text-xs text-mute">
								{formatDay(digest.digestDate)} · updated {formatInstant(digest.generatedAt)}
							</p>
							<h3 className="font-serif text-2xl leading-snug">{digest.headline}</h3>
							<p className="text-sm leading-6 text-paper/80">{digest.overview}</p>
							<p className="text-sm text-mute">{digest.items.length} top stories in this digest</p>
						</div>
					) : (
						<p className="mt-4 text-sm text-mute">
							No digest yet. The pipeline runs automatically — or trigger ingest → enrich → digest from Admin.
						</p>
					)}
				</div>

				<div className="rounded-2xl border border-line bg-panel p-6">
					<h2 className="font-serif text-xl">7-day sentiment</h2>
					<p className="mt-1 text-xs text-mute">How tone shifted across enriched coverage</p>
					<div className="mt-4">
						<Suspense fallback={<p className="text-sm text-mute">Loading chart…</p>}>
							<SentimentChart series={stats?.series ?? []} />
						</Suspense>
					</div>
				</div>
			</section>

			<section className="space-y-4">
				<div className="flex flex-wrap items-end justify-between gap-3">
					<div>
						<h2 className="font-serif text-xl">Latest headlines</h2>
						<p className="mt-1 text-sm text-mute">
							Most recent ingested stories{sourceMix ? ` · ${sourceMix}` : ""}
						</p>
					</div>
					<div className="flex flex-wrap gap-2 text-sm">
						<Link
							className="rounded-full border border-line px-3 py-1 text-mute hover:text-paper"
							to={{ pathname: "/articles", search: params.toString() }}
						>
							All articles
						</Link>
						<Link
							className="rounded-full border border-line px-3 py-1 text-mute hover:text-paper"
							to={{ pathname: "/articles", search: `${params.toString()}${params.toString() ? "&" : ""}source=hackernews` }}
						>
							Hacker News
						</Link>
						<Link
							className="rounded-full border border-line px-3 py-1 text-mute hover:text-paper"
							to={{ pathname: "/articles", search: `${params.toString()}${params.toString() ? "&" : ""}source=gnews` }}
						>
							GNews
						</Link>
						<Link
							className="rounded-full border border-line px-3 py-1 text-mute hover:text-paper"
							to={{ pathname: "/articles", search: `${params.toString()}${params.toString() ? "&" : ""}from=${utcToday()}` }}
						>
							Today
						</Link>
					</div>
				</div>
				{recent.length === 0 ? (
					<EmptyState title="No headlines yet" body="Articles appear after the hourly ingest runs." />
				) : (
					<div className="grid gap-3 lg:grid-cols-2">
						{recent.map((article) => (
							<ArticleCard key={article.id} article={article} compact />
						))}
					</div>
				)}
			</section>
		</div>
	);
}
