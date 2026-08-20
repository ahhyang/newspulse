import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ArticleCard } from "../components/ArticleCard";
import { EmptyState } from "../components/EmptyState";
import { FilterChips } from "../components/FilterChips";
import { api } from "../lib/api";
import { utcDayBounds, utcDaysAgo, utcToday } from "../lib/format";
import type { PageResponse, Article } from "../lib/types";

export function ArticlesPage() {
	const [params, setParams] = useSearchParams();
	const topicId = params.get("topicId") ?? "";
	const [page, setPage] = useState<PageResponse<Article> | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [searchInput, setSearchInput] = useState(params.get("q") ?? "");

	const sentiment = params.get("sentiment") ?? "";
	const source = params.get("source") ?? "";
	const sourceName = params.get("sourceName") ?? "";
	const q = params.get("q") ?? "";
	const from = params.get("from") ?? "";
	const to = params.get("to") ?? "";
	const clusterId = params.get("clusterId") ?? "";
	const pageNo = Number(params.get("page") ?? "0");

	useEffect(() => {
		setSearchInput(q);
	}, [q]);

	useEffect(() => {
		const bounds = utcDayBounds(from || undefined, to || undefined);
		void api
			.articles({
				topicId: topicId || undefined,
				sentiment: sentiment || undefined,
				source: source || undefined,
				sourceName: sourceName || undefined,
				clusterId: clusterId || undefined,
				q: q || undefined,
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
	}, [topicId, sentiment, source, sourceName, q, from, to, clusterId, pageNo]);

	function update(key: string, value: string) {
		const next = new URLSearchParams(params);
		if (value) {
			next.set(key, value);
		} else {
			next.delete(key);
		}
		if (key !== "page") {
			next.delete("page");
		}
		setParams(next);
	}

	function applySearch() {
		update("q", searchInput.trim());
	}

	const sourceChips = useMemo(
		() => [
			{ id: "", label: "All sources", active: !source },
			{ id: "gnews", label: "GNews", active: source === "gnews" },
			{ id: "hackernews", label: "Hacker News", active: source === "hackernews" },
		],
		[source],
	);

	const sentimentChips = useMemo(
		() => [
			{ id: "", label: "All tones", active: !sentiment },
			{ id: "POSITIVE", label: "Positive", active: sentiment === "POSITIVE" },
			{ id: "NEUTRAL", label: "Neutral", active: sentiment === "NEUTRAL" },
			{ id: "NEGATIVE", label: "Negative", active: sentiment === "NEGATIVE" },
		],
		[sentiment],
	);

	const dateChips = useMemo(() => {
		const today = utcToday();
		const weekAgo = utcDaysAgo(7);
		return [
			{ id: "all", label: "All time", active: !from && !to },
			{ id: "today", label: "Today", active: from === today && !to },
			{ id: "week", label: "Last 7 days", active: from === weekAgo && !to },
		];
	}, [from, to]);

	function onDateChip(id: string) {
		const next = new URLSearchParams(params);
		next.delete("page");
		if (id === "all") {
			next.delete("from");
			next.delete("to");
		} else if (id === "today") {
			next.set("from", utcToday());
			next.delete("to");
		} else if (id === "week") {
			next.set("from", utcDaysAgo(7));
			next.delete("to");
		}
		setParams(next);
	}

	return (
		<div className="space-y-6">
			<div className="flex flex-wrap items-end justify-between gap-4">
				<div>
					<p className="text-xs uppercase tracking-[0.2em] text-gold">Coverage</p>
					<h1 className="mt-2 font-serif text-3xl">Articles</h1>
					<p className="mt-2 text-sm text-mute">
						{page ? `${page.totalElements} stories` : "Loading…"} · search, filter by source, sentiment, or date
					</p>
				</div>
			</div>

			<div className="space-y-4 rounded-xl border border-line bg-panel p-4">
				<form
					className="flex flex-col gap-3 sm:flex-row"
					onSubmit={(event) => {
						event.preventDefault();
						applySearch();
					}}
				>
					<input
						className="flex-1 rounded-md border border-line bg-ink px-3 py-2 text-sm text-paper"
						value={searchInput}
						placeholder="Search headlines…"
						onChange={(event) => setSearchInput(event.target.value)}
					/>
					<button
						type="submit"
						className="rounded-md border border-gold/40 bg-gold/10 px-4 py-2 text-sm text-gold hover:bg-gold/20"
					>
						Search
					</button>
				</form>
				<div className="space-y-3">
					<FilterChips chips={sourceChips} onSelect={(id) => update("source", id)} />
					<FilterChips chips={sentimentChips} onSelect={(id) => update("sentiment", id)} />
					<FilterChips chips={dateChips} onSelect={onDateChip} />
				</div>
				<div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
					<label className="text-xs uppercase tracking-wide text-mute">
						Publisher name
						<input
							className="mt-1 w-full rounded-md border border-line bg-ink px-2 py-2 text-sm text-paper"
							value={sourceName}
							placeholder="Reuters, Hacker News…"
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
				</div>
			</div>

			{clusterId ? (
				<p className="text-sm text-mute">
					Showing related coverage (cluster {clusterId}).{" "}
					<button type="button" className="text-gold hover:underline" onClick={() => update("clusterId", "")}>
						Clear
					</button>
				</p>
			) : null}
			{q ? (
				<p className="text-sm text-mute">
					Search: “{q}”{" "}
					<button type="button" className="text-gold hover:underline" onClick={() => update("q", "")}>
						Clear
					</button>
				</p>
			) : null}

			{error ? <EmptyState title="Could not load articles" body={error} /> : null}
			{page && page.content.length === 0 ? (
				<EmptyState title="No matching articles" body="Try another source chip, widen the date range, or clear search." />
			) : null}

			<ul className="space-y-3">
				{page?.content.map((article) => (
					<li key={article.id}>
						<ArticleCard article={article} />
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
