import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { SentimentBadge } from "../components/SentimentBadge";
import { api } from "../lib/api";
import { formatInstant, utcDayBounds } from "../lib/format";
import type { Article, PageResponse, Sentiment } from "../lib/types";

const SENTIMENTS: Array<Sentiment | ""> = ["", "POSITIVE", "NEUTRAL", "NEGATIVE"];

export function ArticlesPage() {
	const [params, setParams] = useSearchParams();
	const topicId = params.get("topicId") ?? "";
	const [page, setPage] = useState<PageResponse<Article> | null>(null);
	const [error, setError] = useState<string | null>(null);

	const sentiment = params.get("sentiment") ?? "";
	const sourceName = params.get("sourceName") ?? "";
	const from = params.get("from") ?? "";
	const to = params.get("to") ?? "";
	const clusterId = params.get("clusterId") ?? "";
	const pageNo = Number(params.get("page") ?? "0");

	useEffect(() => {
		const bounds = utcDayBounds(from || undefined, to || undefined);
		void api
			.articles({
				topicId: topicId || undefined,
				sentiment: sentiment || undefined,
				sourceName: sourceName || undefined,
				clusterId: clusterId || undefined,
				from: bounds.from,
				to: bounds.to,
				page: pageNo,
				size: 20,
			})
			.then((result) => {
				setPage(result);
				setError(null);
			})
			.catch((err: unknown) => setError(err instanceof Error ? err.message : "Failed to load articles"));
	}, [topicId, sentiment, sourceName, from, to, clusterId, pageNo]);

	function update(key: string, value: string) {
		const next = new URLSearchParams(params);
		if (value) {
			next.set(key, value);
		} else {
			next.delete(key);
		}
		next.delete("page");
		setParams(next);
	}

	return (
		<div className="space-y-6">
			<div>
				<p className="text-xs uppercase tracking-[0.2em] text-gold">Coverage</p>
				<h1 className="mt-2 font-serif text-3xl">Articles</h1>
			</div>
			<form className="grid gap-3 rounded-xl border border-line bg-panel p-4 sm:grid-cols-2 lg:grid-cols-4">
				<label className="text-xs uppercase tracking-wide text-mute">
					Sentiment
					<select
						className="mt-1 w-full rounded-md border border-line bg-ink px-2 py-2 text-sm text-paper"
						value={sentiment}
						onChange={(event) => update("sentiment", event.target.value)}
					>
						{SENTIMENTS.map((value) => (
							<option key={value || "all"} value={value}>
								{value || "All"}
							</option>
						))}
					</select>
				</label>
				<label className="text-xs uppercase tracking-wide text-mute">
					Source name
					<input
						className="mt-1 w-full rounded-md border border-line bg-ink px-2 py-2 text-sm text-paper"
						value={sourceName}
						placeholder="Reuters"
						onChange={(event) => update("sourceName", event.target.value)}
					/>
				</label>
				<label className="text-xs uppercase tracking-wide text-mute">
					From
					<input
						type="date"
						className="mt-1 w-full rounded-md border border-line bg-ink px-2 py-2 text-sm text-paper"
						value={from}
						onChange={(event) => update("from", event.target.value)}
					/>
				</label>
				<label className="text-xs uppercase tracking-wide text-mute">
					To
					<input
						type="date"
						className="mt-1 w-full rounded-md border border-line bg-ink px-2 py-2 text-sm text-paper"
						value={to}
						onChange={(event) => update("to", event.target.value)}
					/>
				</label>
			</form>
			{clusterId ? (
				<p className="text-sm text-mute">
					Showing cluster {clusterId}.{" "}
					<button type="button" className="text-gold hover:underline" onClick={() => update("clusterId", "")}>
						Clear
					</button>
				</p>
			) : null}
			{error ? <EmptyState title="Could not load articles" body={error} /> : null}
			{page && page.content.length === 0 ? (
				<EmptyState title="No matching articles" body="Widen the filters or run ingestion from Admin." />
			) : null}
			<ul className="space-y-3">
				{page?.content.map((article) => (
					<li key={article.id} className="rounded-xl border border-line bg-panel p-5">
						<div className="flex flex-wrap items-center justify-between gap-2">
							<p className="text-xs text-mute">
								{article.sourceName} · {formatInstant(article.publishedAt)}
							</p>
							<SentimentBadge sentiment={article.sentiment} />
						</div>
						<Link to={`/articles/${article.id}`} className="mt-2 block font-serif text-xl hover:text-gold">
							{article.title}
						</Link>
						{article.summary ? <p className="mt-2 text-sm leading-6 text-paper/80">{article.summary}</p> : null}
						<a className="mt-3 inline-block text-sm text-gold hover:underline" href={article.url} target="_blank" rel="noreferrer">
							Original source
						</a>
					</li>
				))}
			</ul>
			{page && page.totalPages > 1 ? (
				<div className="flex items-center justify-between text-sm text-mute">
					<span>
						Page {page.page + 1} of {page.totalPages}
					</span>
					<div className="flex gap-2">
						<button
							type="button"
							disabled={page.page === 0}
							className="rounded-md border border-line px-3 py-1 disabled:opacity-40"
							onClick={() => update("page", String(page.page - 1))}
						>
							Previous
						</button>
						<button
							type="button"
							disabled={page.last}
							className="rounded-md border border-line px-3 py-1 disabled:opacity-40"
							onClick={() => update("page", String(page.page + 1))}
						>
							Next
						</button>
					</div>
				</div>
			) : null}
		</div>
	);
}
