import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArticleCard } from "../components/ArticleCard";
import { EmptyState } from "../components/EmptyState";
import { SentimentBadge } from "../components/SentimentBadge";
import { SourceBadge } from "../components/SourceBadge";
import { api } from "../lib/api";
import { formatInstant } from "../lib/format";
import type { Article } from "../lib/types";

export function ArticlePage() {
	const { id } = useParams();
	const [article, setArticle] = useState<Article | null>(null);
	const [related, setRelated] = useState<Article[]>([]);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		if (!id) {
			return;
		}
		void api
			.article(Number(id))
			.then((loaded) => {
				setArticle(loaded);
				if (loaded.clusterId) {
					return api
						.articles({ clusterId: loaded.clusterId, size: 6 })
						.then((page) => setRelated(page.content.filter((item) => item.id !== loaded.id)));
				}
				setRelated([]);
			})
			.catch((err: unknown) => setError(err instanceof Error ? err.message : "Not found"));
	}, [id]);

	if (error) {
		return <EmptyState title="Article not found" body={error} />;
	}
	if (!article) {
		return <p className="text-sm text-mute">Loading article…</p>;
	}

	return (
		<div className="mx-auto max-w-3xl space-y-8">
			<Link to="/articles" className="text-sm text-gold hover:underline">
				← Back to articles
			</Link>
			<article className="space-y-5">
				<div className="flex flex-wrap items-center gap-2">
					<SourceBadge source={article.source} sourceName={article.sourceName} />
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
				{article.summary ? (
					<div className="rounded-xl border border-line bg-panel p-5">
						<p className="text-xs uppercase tracking-wide text-gold">AI summary</p>
						<p className="mt-2 text-base leading-7 text-paper/90">{article.summary}</p>
					</div>
				) : (
					<p className="text-sm text-mute">Summary pending — enrichment runs every few minutes.</p>
				)}
				{article.sentimentJustification ? (
					<blockquote className="border-l-2 border-gold/50 pl-4 text-sm leading-6 text-mute">
						<span className="text-xs uppercase tracking-wide text-gold">Why this sentiment</span>
						<p className="mt-2">{article.sentimentJustification}</p>
					</blockquote>
				) : null}
				<a
					className="inline-flex items-center gap-2 rounded-md border border-gold/40 bg-gold/10 px-4 py-2 text-sm text-gold hover:bg-gold/20"
					href={article.url}
					target="_blank"
					rel="noreferrer"
				>
					Read original article ↗
				</a>
			</article>

			{related.length > 0 ? (
				<section className="space-y-3 border-t border-line pt-8">
					<h2 className="font-serif text-xl">Related coverage</h2>
					<p className="text-sm text-mute">Other sources reporting the same story cluster</p>
					<ul className="space-y-3">
						{related.map((item) => (
							<li key={item.id}>
								<ArticleCard article={item} compact />
							</li>
						))}
					</ul>
				</section>
			) : null}
		</div>
	);
}
