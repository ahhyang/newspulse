const SOURCE_STYLES: Record<string, string> = {
	gnews: "border-sky-400/30 bg-sky-400/10 text-sky-300",
	hackernews: "border-orange-400/30 bg-orange-400/10 text-orange-300",
};

export function SourceBadge({ source, sourceName }: { source: string; sourceName?: string }) {
	const key = source.toLowerCase();
	const label =
		key === "hackernews" ? "Hacker News" : key === "gnews" ? "GNews" : sourceName ?? source;
	const style = SOURCE_STYLES[key] ?? "border-line bg-panel-2 text-mute";
	return (
		<span className={`rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide ${style}`}>
			{label}
		</span>
	);
}
