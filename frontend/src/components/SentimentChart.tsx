import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { SentimentPoint } from "../lib/types";

type Props = {
	series: SentimentPoint[];
};

export default function SentimentChart({ series }: Props) {
	if (series.length === 0) {
		return <p className="text-sm text-mute">No enriched articles in this window yet.</p>;
	}
	return (
		<div className="h-56 w-full">
			<ResponsiveContainer width="100%" height="100%">
				<BarChart data={series} barCategoryGap="28%">
					<CartesianGrid stroke="#2a3646" vertical={false} />
					<XAxis
						dataKey="date"
						tick={{ fill: "#8b9bb0", fontSize: 11 }}
						tickFormatter={(value: string) => value.slice(5)}
						axisLine={{ stroke: "#2a3646" }}
						tickLine={false}
					/>
					<YAxis
						allowDecimals={false}
						tick={{ fill: "#8b9bb0", fontSize: 11 }}
						axisLine={false}
						tickLine={false}
						width={28}
					/>
					<Tooltip
						cursor={{ fill: "rgba(212, 160, 23, 0.08)" }}
						contentStyle={{
							background: "#141b24",
							border: "1px solid #2a3646",
							borderRadius: 8,
							color: "#e8eef4",
							fontSize: 12,
						}}
					/>
					<Bar dataKey="positive" stackId="s" fill="#3dba8b" name="Positive" />
					<Bar dataKey="neutral" stackId="s" fill="#c9a227" name="Neutral" />
					<Bar dataKey="negative" stackId="s" fill="#e05d5d" name="Negative" radius={[3, 3, 0, 0]} />
				</BarChart>
			</ResponsiveContainer>
		</div>
	);
}
