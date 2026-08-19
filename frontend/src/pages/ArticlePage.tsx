import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { SentimentBadge } from "../components/SentimentBadge";
import { api } from "../lib/api";
import { formatInstant } from "../lib/format";
import type { Article } from "../lib/types";

export function ArticlePage() {
	const { id } = useParams();
	const [article, setArticle] = useState<Article | null>(null);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		if (!id) {
			return;
		}
		void api
			.article(Number(id))
			.then(setArticle)
			.catch((err: unknown) => setError(err instanceof Error ? err.message : "Not found"));
	}, [id]);

	if (error) {
		return <EmptyState title="Article not found" body={error} />;
	}
	if (!article) {
		return <p className="text-sm text-mute">Loading article…</p>;
	}

	return (
		<article className="mx-auto max-w-3xl space-y-5">
			<Link to="/articles" className="text-sm text-gold hover:underline">
				← Articles
			</Link>
			<div className="flex flex-wrap items-center gap-2">
				<SentimentBadge sentiment={article.sentiment} />
				{article.stanceTag ? (
					<span className="rounded-full border border-line px-2 py-0.5 text-[11px] uppercase tracking-wide text-mute">
						{article.stanceTag}
					</span>
				) : null}
			</div>
			<h1 className="font-serif text-4xl leading-tight">{article.title}</h1>
			<p className="text-sm text-mute">
				{article.sourceName} · {formatInstant(article.publishedAt)} · {article.topicName}
			</p>
			{article.summary ? <p className="text-base leading-7 text-paper/90">{article.summary}</p> : null}
			{article.sentimentJustification ? (
				<blockquote className="border-l-2 border-gold/50 pl-4 text-sm leading-6 text-mute">
					{article.sentimentJustification}
				</blockquote>
			) : null}
			<a className="inline-block text-gold hover:underline" href={article.url} target="_blank" rel="noreferrer">
				Read original
			</a>
		</article>
	);
}
