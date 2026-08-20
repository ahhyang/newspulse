import { Link } from "react-router-dom";
import { formatInstant, relativeTime } from "../lib/format";
import type { Article } from "../lib/types";
import { SentimentBadge } from "./SentimentBadge";
import { SourceBadge } from "./SourceBadge";

export function ArticleCard({ article, compact = false }: { article: Article; compact?: boolean }) {
	return (
		<article className="rounded-xl border border-line bg-panel p-5 transition hover:border-gold/30">
			<div className="flex flex-wrap items-center justify-between gap-2">
				<div className="flex flex-wrap items-center gap-2">
					<SourceBadge source={article.source} sourceName={article.sourceName} />
					<span className="text-xs text-mute" title={formatInstant(article.publishedAt)}>
						{relativeTime(article.publishedAt)}
					</span>
				</div>
				<SentimentBadge sentiment={article.sentiment} />
			</div>
			<Link
				to={`/articles/${article.id}`}
				className={`mt-3 block font-serif hover:text-gold ${compact ? "text-lg leading-snug" : "text-xl"}`}
			>
				{article.title}
			</Link>
			{article.summary ? (
				<p className={`mt-2 leading-6 text-paper/80 ${compact ? "line-clamp-2 text-sm" : "text-sm"}`}>
					{article.summary}
				</p>
			) : (
				<p className="mt-2 text-sm text-mute">Waiting for AI enrichment…</p>
			)}
			<div className="mt-4 flex flex-wrap items-center gap-3 text-sm">
				<span className="text-mute">{article.topicName}</span>
				{article.stanceTag ? (
					<span className="rounded-full border border-line px-2 py-0.5 text-[11px] uppercase tracking-wide text-mute">
						{article.stanceTag}
					</span>
				) : null}
				<a className="text-gold hover:underline" href={article.url} target="_blank" rel="noreferrer">
					Open original ↗
				</a>
			</div>
		</article>
	);
}
