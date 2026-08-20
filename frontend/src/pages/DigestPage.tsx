import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { SentimentBadge } from "../components/SentimentBadge";
import { useAutoRefresh } from "../hooks/useAutoRefresh";
import { ApiRequestError, api } from "../lib/api";
import { formatDay, formatInstant, formatPct, shiftDay } from "../lib/format";
import type { Digest, Stats } from "../lib/types";

const SentimentChart = lazy(() => import("../components/SentimentChart"));

export function DigestPage() {
	const [params, setParams] = useSearchParams();
	const topicId = params.get("topicId") ? Number(params.get("topicId")) : undefined;
	const date = params.get("date");
	const { tick, refreshing, refresh } = useAutoRefresh(300_000);
	const [digest, setDigest] = useState<Digest | null>(null);
	const [stats, setStats] = useState<Stats | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setError(null);
		const digestCall = date ? api.digestByDate(date, topicId) : api.digestLatest(topicId);
		void Promise.allSettled([digestCall, api.stats(topicId)])
			.then(([digestResult, statsResult]) => {
				if (cancelled) {
					return;
				}
				if (statsResult.status === "fulfilled") {
					setStats(statsResult.value);
				}
				if (digestResult.status === "fulfilled") {
					setDigest(digestResult.value);
					return;
				}
				setDigest(null);
				const reason = digestResult.reason;
				if (reason instanceof ApiRequestError && reason.status === 404) {
					setError(null);
					return;
				}
				setError(reason instanceof Error ? reason.message : "Could not load digest");
			})
			.finally(() => {
				if (!cancelled) {
					setLoading(false);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [date, topicId, tick]);

	const mix = useMemo(() => {
		if (!digest) {
			return [];
		}
		return [
			{ label: "Positive", value: formatPct(digest.positivePct), color: "bg-pos" },
			{ label: "Neutral", value: formatPct(digest.neutralPct), color: "bg-neu" },
			{ label: "Negative", value: formatPct(digest.negativePct), color: "bg-neg" },
		];
	}, [digest]);

	function goTo(nextDate: string) {
		const next = new URLSearchParams(params);
		next.set("date", nextDate);
		setParams(next);
	}

	if (loading) {
		return <p className="text-sm text-mute">Loading briefing…</p>;
	}

	if (error) {
		return <EmptyState title="Digest unavailable" body={error} />;
	}

	if (!digest) {
		return (
			<EmptyState
				title="No briefing yet"
				body="News is ingested hourly from GNews and Hacker News, summarized automatically, and compiled into a daily digest. Check back soon or run the pipeline from Admin."
			/>
		);
	}

	return (
		<div className="space-y-8">
			<section className="rounded-2xl border border-line bg-panel p-6 shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]">
				<div className="flex flex-wrap items-start justify-between gap-4">
					<div>
						<p className="text-xs uppercase tracking-[0.2em] text-gold">Daily briefing</p>
						<h1 className="mt-2 max-w-3xl font-serif text-3xl leading-tight text-paper">{digest.headline}</h1>
						<p className="mt-2 text-sm text-mute">
							{formatDay(digest.digestDate)} · {digest.topicName} · generated {formatInstant(digest.generatedAt)}
						</p>
					</div>
					<div className="flex flex-wrap items-center gap-2">
						<button
							type="button"
							className="rounded-md border border-line px-3 py-1 text-sm text-mute hover:text-paper"
							onClick={refresh}
						>
							{refreshing ? "Refreshing…" : "Refresh"}
						</button>
						<button
							type="button"
							className="rounded-md border border-line px-3 py-1 text-sm text-mute hover:text-paper"
							onClick={() => goTo(shiftDay(digest.digestDate, -1))}
						>
							Previous
						</button>
						<button
							type="button"
							className="rounded-md border border-line px-3 py-1 text-sm text-mute hover:text-paper"
							onClick={() => goTo(shiftDay(digest.digestDate, 1))}
						>
							Next
						</button>
					</div>
				</div>
				<p className="mt-4 max-w-3xl text-sm leading-6 text-paper/80">{digest.overview}</p>
				<div className="mt-5 flex flex-wrap gap-6">
					{mix.map((item) => (
						<div key={item.label}>
							<p className="text-xs uppercase tracking-wide text-mute">{item.label}</p>
							<p className="mt-1 font-serif text-2xl">{item.value}</p>
							<div className={`mt-2 h-1 w-16 rounded-full ${item.color}`} />
						</div>
					))}
				</div>
			</section>

			<section className="rounded-2xl border border-line bg-panel p-6">
				<div className="mb-4 flex items-baseline justify-between">
					<h2 className="font-serif text-xl">Sentiment trend</h2>
					<p className="text-xs text-mute">{stats ? `${stats.articleCount} enriched articles` : "Last 7 UTC days"}</p>
				</div>
				<Suspense fallback={<p className="text-sm text-mute">Loading chart…</p>}>
					<SentimentChart series={stats?.series ?? []} />
				</Suspense>
			</section>

			<section className="space-y-3">
				<h2 className="font-serif text-xl">Top stories</h2>
				{digest.items.length === 0 ? (
					<EmptyState title="Quiet day" body="A digest was written, but no enriched articles landed in this UTC window." />
				) : (
					digest.items.map((item) => (
						<article key={item.rank} className="rounded-xl border border-line bg-panel p-5">
							<div className="flex flex-wrap items-start justify-between gap-3">
								<p className="text-xs text-gold">#{item.rank}</p>
								<SentimentBadge sentiment={item.sentiment} />
							</div>
							<h3 className="mt-2 font-serif text-2xl leading-snug">{item.title}</h3>
							<p className="mt-2 text-sm leading-6 text-paper/80">{item.summary}</p>
							<div className="mt-4 flex flex-wrap items-center gap-3 text-sm">
								<span className="text-mute">
									{item.sourceCount} {item.sourceCount === 1 ? "source" : "sources"} covered this
								</span>
								{item.clusterId ? (
									<Link
										className="text-gold hover:underline"
										to={`/articles?clusterId=${item.clusterId}${topicId ? `&topicId=${topicId}` : ""}`}
									>
										See all coverage
									</Link>
								) : null}
							</div>
						</article>
					))
				)}
			</section>
		</div>
	);
}
