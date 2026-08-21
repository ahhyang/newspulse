import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { BatchSummaryPanel } from "../components/BatchSummaryPanel";
import { ArticleCard } from "../components/ArticleCard";
import { EmptyState } from "../components/EmptyState";
import { FilterChips } from "../components/FilterChips";
import { api } from "../lib/api";
import { utcDayBounds, utcDaysAgo, utcToday } from "../lib/format";
import type { BatchSummary, PageResponse, Article } from "../lib/types";

const MAX_SELECTION = 12;

export function ArticlesPage() {
	const [params, setParams] = useSearchParams();
	const topicId = params.get("topicId") ?? "";
	const [page, setPage] = useState<PageResponse<Article> | null>(null);
	const [error, setError] = useState<string | null>(null);
	const [searchInput, setSearchInput] = useState(params.get("q") ?? "");
	const [selectedIds, setSelectedIds] = useState<number[]>([]);
	const [batchResult, setBatchResult] = useState<BatchSummary | null>(null);
	const [batchLoading, setBatchLoading] = useState(false);
	const [batchError, setBatchError] = useState<string | null>(null);

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

	function toggleSelection(id: number) {
		setSelectedIds((current) => {
			if (current.includes(id)) {
				return current.filter((value) => value !== id);
			}
			if (current.length >= MAX_SELECTION) {
				return current;
			}
			return [...current, id];
		});
	}

	function selectAllOnPage() {
		if (!page) {
			return;
		}
		setSelectedIds((current) => {
			const merged = new Set(current);
			for (const article of page.content) {
				if (merged.size >= MAX_SELECTION) {
					break;
				}
				merged.add(article.id);
			}
			return Array.from(merged);
		});
	}

	async function summarizeSelected() {
		if (selectedIds.length < 2) {
			return;
		}
		setBatchLoading(true);
		setBatchError(null);
		setBatchResult(null);
		try {
			const result = await api.summarizeBatch(selectedIds);
			setBatchResult(result);
		} catch (err) {
			setBatchError(err instanceof Error ? err.message : "Batch summary failed");
		} finally {
			setBatchLoading(false);
		}
	}

	return (
		<div className="space-y-6">
			<div className="flex flex-wrap items-end justify-between gap-4">
				<div>
					<p className="text-xs uppercase tracking-[0.2em] text-gold">Coverage</p>
					<h1 className="mt-2 font-serif text-3xl">Articles</h1>
					<p className="mt-2 text-sm text-mute">
						{page ? `${page.totalElements} stories` : "Loading…"} · AI is click-only: tick 2–12 articles →
						Summarize selected, or Admin → Enrich pending
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

			<ul className="space-y-3 pb-24">
				{page?.content.map((article) => (
					<li key={article.id}>
						<ArticleCard
							article={article}
							selectable
							selected={selectedIds.includes(article.id)}
							onToggle={toggleSelection}
						/>
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

			{selectedIds.length > 0 ? (
				<div className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-ink/95 px-4 py-3 backdrop-blur">
					<div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
						<p className="text-sm text-paper">
							{selectedIds.length} selected {selectedIds.length < 2 ? "(pick at least 2)" : ""}
							{selectedIds.length >= MAX_SELECTION ? ` · max ${MAX_SELECTION}` : ""}
						</p>
						<div className="flex flex-wrap gap-2">
							<button
								type="button"
								className="rounded-md border border-line px-3 py-1.5 text-sm text-mute hover:text-paper"
								onClick={selectAllOnPage}
							>
								Select page
							</button>
							<button
								type="button"
								className="rounded-md border border-line px-3 py-1.5 text-sm text-mute hover:text-paper"
								onClick={() => setSelectedIds([])}
							>
								Clear
							</button>
							<button
								type="button"
								disabled={selectedIds.length < 2 || batchLoading}
								className="rounded-md border border-gold/40 bg-gold/15 px-4 py-1.5 text-sm text-gold hover:bg-gold/25 disabled:opacity-40"
								onClick={() => void summarizeSelected()}
							>
								{batchLoading ? "Summarizing…" : "Summarize selected"}
							</button>
						</div>
					</div>
				</div>
			) : null}

			<BatchSummaryPanel
				result={batchResult}
				loading={batchLoading}
				error={batchError}
				onClose={() => {
					setBatchResult(null);
					setBatchError(null);
				}}
			/>
		</div>
	);
}
