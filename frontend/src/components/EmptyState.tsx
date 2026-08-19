export function EmptyState({ title, body }: { title: string; body: string }) {
	return (
		<div className="rounded-xl border border-dashed border-line bg-panel/60 px-6 py-12 text-center">
			<p className="font-serif text-xl text-paper">{title}</p>
			<p className="mx-auto mt-2 max-w-md text-sm leading-6 text-mute">{body}</p>
		</div>
	);
}
