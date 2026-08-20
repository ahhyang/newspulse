import { useCallback, useEffect, useState } from "react";

export function useAutoRefresh(intervalMs: number, enabled = true) {
	const [tick, setTick] = useState(0);
	const [refreshing, setRefreshing] = useState(false);

	const refresh = useCallback(() => {
		setRefreshing(true);
		setTick((value) => value + 1);
	}, []);

	useEffect(() => {
		if (!enabled) {
			return;
		}
		const timer = window.setInterval(refresh, intervalMs);
		return () => window.clearInterval(timer);
	}, [enabled, intervalMs, refresh]);

	useEffect(() => {
		if (tick === 0) {
			return;
		}
		const timer = window.setTimeout(() => setRefreshing(false), 400);
		return () => window.clearTimeout(timer);
	}, [tick]);

	return { tick, refreshing, refresh };
}
