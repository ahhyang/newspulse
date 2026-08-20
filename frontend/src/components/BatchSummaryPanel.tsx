import type { BatchSummary } from "../lib/types";

export function BatchSummaryPanel({
	result,
	loading,
	error,
	onClose,
}: {
	result: BatchSummary | null;
	loading: boolean;
	error: string | null;
	onClose: () => void;
}) {
	if (!loading && !result && !error) {
		return null;
	}

	return (
		<div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 p-4 sm:items-center">
			<div
				role="dialog"
				aria-modal="true"
				className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-line bg-panel p-6 shadow-2xl"
			>
				<div className="flex items-start justify-between gap-4">
					<div>
						<p className="text-xs uppercase tracking-[0.2em] text-gold">Custom briefing</p>
						<h2 className="mt-2 font-serif text-2xl leading-snug">
							{loading ? "Composing your summary…" : result?.headline ?? "Could not summarize"}
						</h2>
					</div>
					<button
						type="button"
						onClick={onClose}
						className="rounded-md border border-line px-2 py-1 text-sm text-mute hover:text-paper"
					>
						Close
					</button>
				</div>

				{loading ? (
					<p className="mt-6 text-sm text-mute">AI is reading your selected stories and writing a combined overview…</p>
				) : null}

				{error ? <p className="mt-6 text-sm text-neg">{error}</p> : null}

				{result ? (
					<div className="mt-6 space-y-5">
						<p className="text-sm leading-7 text-paper/90">{result.overview}</p>
						{result.themes.length > 0 ? (
							<div className="flex flex-wrap gap-2">
								{result.themes.map((theme) => (
									<span
										key={theme}
										className="rounded-full border border-gold/30 bg-gold/10 px-3 py-1 text-xs text-gold"
									>
										{theme}
									</span>
								))}
							</div>
						) : null}
						<div>
							<p className="text-xs uppercase tracking-wide text-mute">
								Based on {result.articleCount} selected stories
							</p>
							<ul className="mt-3 space-y-2">
								{result.articles.map((article) => (
									<li key={article.id} className="rounded-lg border border-line bg-ink/40 px-3 py-2 text-sm">
										<span className="text-mute">{article.sourceName} · </span>
										{article.title}
									</li>
								))}
							</ul>
						</div>
					</div>
				) : null}
			</div>
		</div>
	);
}
