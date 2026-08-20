export function StatCard({
	label,
	value,
	hint,
	accent,
}: {
	label: string;
	value: string | number;
	hint?: string;
	accent?: "pos" | "neu" | "neg" | "gold";
}) {
	const accentClass =
		accent === "pos"
			? "text-pos"
			: accent === "neu"
				? "text-neu"
				: accent === "neg"
					? "text-neg"
					: accent === "gold"
						? "text-gold"
						: "text-paper";
	return (
		<div className="rounded-xl border border-line bg-panel p-4">
			<p className="text-xs uppercase tracking-wide text-mute">{label}</p>
			<p className={`mt-2 font-serif text-3xl leading-none ${accentClass}`}>{value}</p>
			{hint ? <p className="mt-2 text-xs leading-5 text-mute">{hint}</p> : null}
		</div>
	);
}
