import type { Sentiment } from "../lib/types";

const STYLES: Record<Sentiment, string> = {
	POSITIVE: "border-pos/40 bg-pos/10 text-pos",
	NEUTRAL: "border-neu/40 bg-neu/10 text-neu",
	NEGATIVE: "border-neg/40 bg-neg/10 text-neg",
};

export function SentimentBadge({ sentiment }: { sentiment: Sentiment | null }) {
	if (!sentiment) {
		return (
			<span className="rounded-full border border-line px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide text-mute">
				Unscored
			</span>
		);
	}
	return (
		<span
			className={`rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide ${STYLES[sentiment]}`}
		>
			{sentiment.toLowerCase()}
		</span>
	);
}
