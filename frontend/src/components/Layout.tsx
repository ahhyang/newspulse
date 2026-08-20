import { NavLink, Outlet, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useEffect, useState } from "react";
import { api } from "../lib/api";
import type { Topic } from "../lib/types";

const navClass = ({ isActive }: { isActive: boolean }) =>
	`rounded-md px-3 py-1.5 text-sm ${isActive ? "bg-panel-2 text-paper" : "text-mute hover:text-paper"}`;

export function Layout() {
	const { signedIn, signOut } = useAuth();
	const [topics, setTopics] = useState<Topic[]>([]);
	const [params, setParams] = useSearchParams();
	const topicId = params.get("topicId") ?? "";

	useEffect(() => {
		void api.topics().then(setTopics).catch(() => setTopics([]));
	}, []);

	function onTopicChange(value: string) {
		const next = new URLSearchParams(params);
		if (value) {
			next.set("topicId", value);
		} else {
			next.delete("topicId");
		}
		setParams(next, { replace: true });
	}

	return (
		<div className="min-h-screen bg-ink">
			<div className="pointer-events-none fixed inset-x-0 top-0 h-px bg-linear-to-r from-transparent via-gold to-transparent" />
			<header className="sticky top-0 z-20 border-b border-line/80 bg-ink/90 backdrop-blur">
				<div className="mx-auto flex max-w-6xl flex-wrap items-center gap-4 px-4 py-3">
					<NavLink to="/" className="flex items-center gap-2">
						<span className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-gold/40 text-gold">
							●
						</span>
						<span className="font-serif text-lg tracking-tight">NewsPulse</span>
					</NavLink>
					<nav className="flex items-center gap-1">
						<NavLink to={{ pathname: "/", search: params.toString() }} className={navClass} end>
							Overview
						</NavLink>
						<NavLink to={{ pathname: "/digest", search: params.toString() }} className={navClass}>
							Digest
						</NavLink>
						<NavLink to={{ pathname: "/articles", search: params.toString() }} className={navClass}>
							Articles
						</NavLink>
						<NavLink to={{ pathname: "/admin", search: params.toString() }} className={navClass}>
							Admin
						</NavLink>
					</nav>
					<div className="ml-auto flex items-center gap-3">
						<label className="text-xs uppercase tracking-wide text-mute">
							Topic
							<select
								className="ml-2 rounded-md border border-line bg-panel px-2 py-1 text-sm text-paper"
								value={topicId}
								onChange={(event) => onTopicChange(event.target.value)}
							>
								<option value="">All active</option>
								{topics.map((topic) => (
									<option key={topic.id} value={topic.id}>
										{topic.name}
									</option>
								))}
							</select>
						</label>
						{signedIn ? (
							<button type="button" onClick={signOut} className="text-xs text-mute hover:text-paper">
								Sign out
							</button>
						) : null}
					</div>
				</div>
			</header>
			<main className="mx-auto max-w-6xl px-4 py-8">
				<Outlet />
			</main>
			<footer className="border-t border-line/60 py-6 text-center text-xs text-mute">
				Auto-ingest hourly · AI enrichment every ~2 min · daily digest at 00:05 UTC · GNews + Hacker News
			</footer>
		</div>
	);
}
