export type FilterChip = {
	id: string;
	label: string;
	active?: boolean;
};

export function FilterChips({
	chips,
	onSelect,
}: {
	chips: FilterChip[];
	onSelect: (id: string) => void;
}) {
	return (
		<div className="flex flex-wrap gap-2">
			{chips.map((chip) => (
				<button
					key={chip.id}
					type="button"
					onClick={() => onSelect(chip.id)}
					className={`rounded-full border px-3 py-1 text-xs transition ${
						chip.active
							? "border-gold/50 bg-gold/15 text-gold"
							: "border-line bg-panel-2 text-mute hover:border-gold/30 hover:text-paper"
					}`}
				>
					{chip.label}
				</button>
			))}
		</div>
	);
}
