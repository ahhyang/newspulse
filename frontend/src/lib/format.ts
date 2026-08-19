export function formatPct(value: string | number | null | undefined): string {
	if (value == null || value === "") {
		return "0%";
	}
	const n = typeof value === "number" ? value : Number(value);
	if (Number.isNaN(n)) {
		return "0%";
	}
	const rounded = Math.round(n * 100) / 100;
	const text = Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2).replace(/0+$/, "").replace(/\.$/, "");
	return `${text}%`;
}

export function shiftDay(isoDate: string, delta: number): string {
	const day = new Date(`${isoDate}T00:00:00.000Z`);
	day.setUTCDate(day.getUTCDate() + delta);
	return day.toISOString().slice(0, 10);
}

export function formatDay(isoDate: string): string {
	return new Date(`${isoDate}T00:00:00.000Z`).toLocaleDateString("en-GB", {
		weekday: "long",
		day: "numeric",
		month: "long",
		year: "numeric",
		timeZone: "UTC",
	});
}

export function formatInstant(iso: string | null | undefined): string {
	if (!iso) {
		return "—";
	}
	return (
		new Date(iso).toLocaleString("en-GB", {
			day: "numeric",
			month: "short",
			year: "numeric",
			hour: "2-digit",
			minute: "2-digit",
			timeZone: "UTC",
		}) + " UTC"
	);
}

export function buildQuery(params: Record<string, string | number | boolean | null | undefined>): string {
	const search = new URLSearchParams();
	for (const [key, value] of Object.entries(params)) {
		if (value === undefined || value === null || value === "") {
			continue;
		}
		search.set(key, String(value));
	}
	const encoded = search.toString();
	return encoded ? `?${encoded}` : "";
}

export function utcDayBounds(from?: string, to?: string): { from?: string; to?: string } {
	return {
		from: from ? `${from}T00:00:00Z` : undefined,
		to: to ? `${to}T23:59:59Z` : undefined,
	};
}
