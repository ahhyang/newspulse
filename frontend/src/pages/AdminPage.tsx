import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../context/AuthContext";
import { ApiRequestError, api } from "../lib/api";
import type { Topic } from "../lib/types";

export function AdminPage() {
	const { signedIn, signIn, signOut } = useAuth();
	const [username, setUsername] = useState("admin");
	const [password, setPassword] = useState("");
	const [authError, setAuthError] = useState<string | null>(null);
	const [log, setLog] = useState<string | null>(null);
	const [busy, setBusy] = useState(false);
	const [topics, setTopics] = useState<Topic[]>([]);
	const [name, setName] = useState("");
	const [query, setQuery] = useState("");
	const [description, setDescription] = useState("");

	async function refreshTopics() {
		setTopics(await api.topics());
	}

	useEffect(() => {
		void refreshTopics().catch(() => setTopics([]));
	}, []);

	async function onLogin(event: FormEvent) {
		event.preventDefault();
		setAuthError(null);
		try {
			const result = await api.login(username, password);
			signIn(result.accessToken);
			setPassword("");
		} catch (err) {
			setAuthError(err instanceof Error ? err.message : "Login failed");
		}
	}

	async function run(label: string, action: () => Promise<unknown>) {
		setBusy(true);
		setLog(null);
		try {
			const result = await action();
			setLog(`${label} finished\n${JSON.stringify(result, null, 2)}`);
			await refreshTopics();
		} catch (err) {
			if (err instanceof ApiRequestError && err.status === 401) {
				signOut();
			}
			const message = err instanceof ApiRequestError && err.status === 401
				? "Sign in required for admin actions."
				: err instanceof Error
					? err.message
					: "Request failed";
			setLog(`${label} failed: ${message}`);
		} finally {
			setBusy(false);
		}
	}

	async function onCreate(event: FormEvent) {
		event.preventDefault();
		await run("Create topic", () => api.createTopic({ name, query, description: description || undefined }));
		setName("");
		setQuery("");
		setDescription("");
	}

	if (!signedIn) {
		return (
			<div className="mx-auto max-w-md rounded-2xl border border-line bg-panel p-6">
				<p className="text-xs uppercase tracking-[0.2em] text-gold">Admin</p>
				<h1 className="mt-2 font-serif text-3xl">Sign in</h1>
				<p className="mt-2 text-sm text-mute">JWT is required for ingestion, enrichment, digest runs, and topic writes.</p>
				<form className="mt-6 space-y-3" onSubmit={(event) => void onLogin(event)}>
					<label className="block text-xs uppercase tracking-wide text-mute">
						Username
						<input
							className="mt-1 w-full rounded-md border border-line bg-ink px-3 py-2 text-sm text-paper"
							value={username}
							onChange={(event) => setUsername(event.target.value)}
						/>
					</label>
					<label className="block text-xs uppercase tracking-wide text-mute">
						Password
						<input
							type="password"
							className="mt-1 w-full rounded-md border border-line bg-ink px-3 py-2 text-sm text-paper"
							value={password}
							onChange={(event) => setPassword(event.target.value)}
						/>
					</label>
					{authError ? <p className="text-sm text-neg">{authError}</p> : null}
					<button
						type="submit"
						className="w-full rounded-md bg-gold px-3 py-2 text-sm font-medium text-ink hover:bg-gold/90"
					>
						Get token
					</button>
				</form>
			</div>
		);
	}

	return (
		<div className="space-y-8">
			<div>
				<p className="text-xs uppercase tracking-[0.2em] text-gold">Operations</p>
				<h1 className="mt-2 font-serif text-3xl">Admin</h1>
			</div>
			<section className="rounded-2xl border border-line bg-panel p-5">
				<h2 className="font-serif text-xl">Pipeline</h2>
				<p className="mt-1 text-sm text-mute">
					News still auto-ingests hourly. <strong className="text-paper">AI only runs when you click</strong> —
					Enrich pending (summarize new articles) or use Articles → Summarize selected.
				</p>
				<div className="mt-4 flex flex-wrap gap-2">
					<button
						type="button"
						disabled={busy}
						className="rounded-md border border-line px-3 py-2 text-sm hover:border-gold"
						onClick={() => void run("Ingestion", () => api.ingest())}
					>
						Ingest now
					</button>
					<button
						type="button"
						disabled={busy}
						className="rounded-md border border-gold/40 bg-gold/15 px-3 py-2 text-sm text-gold hover:bg-gold/25"
						onClick={() => void run("Enrichment", () => api.enrich())}
					>
						Enrich pending (AI)
					</button>
					<button
						type="button"
						disabled={busy}
						className="rounded-md border border-line px-3 py-2 text-sm hover:border-gold"
						onClick={() => void run("Digest", () => api.generateDigest())}
					>
						Generate today's digest
					</button>
				</div>
				{log ? (
					<pre className="mt-4 overflow-x-auto rounded-md bg-ink p-3 text-xs text-paper/80">{log}</pre>
				) : null}
			</section>
			<section className="rounded-2xl border border-line bg-panel p-5">
				<h2 className="font-serif text-xl">Topics</h2>
				<ul className="mt-4 divide-y divide-line">
					{topics.map((topic) => (
						<li key={topic.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
							<div>
								<p className="font-medium">{topic.name}</p>
								<p className="text-sm text-mute">{topic.query}</p>
							</div>
							<button
								type="button"
								className="rounded-md border border-line px-3 py-1 text-xs"
								onClick={() => void run(topic.active ? "Pause topic" : "Activate topic", () =>
									api.updateTopic(topic.id, { active: !topic.active })
								)}
							>
								{topic.active ? "Active" : "Paused"}
							</button>
						</li>
					))}
				</ul>
				<form className="mt-6 grid gap-3 sm:grid-cols-2" onSubmit={(event) => void onCreate(event)}>
					<input
						required
						placeholder="Name"
						className="rounded-md border border-line bg-ink px-3 py-2 text-sm"
						value={name}
						onChange={(event) => setName(event.target.value)}
					/>
					<input
						required
						placeholder="GNews query"
						className="rounded-md border border-line bg-ink px-3 py-2 text-sm"
						value={query}
						onChange={(event) => setQuery(event.target.value)}
					/>
					<input
						placeholder="Description (optional)"
						className="rounded-md border border-line bg-ink px-3 py-2 text-sm sm:col-span-2"
						value={description}
						onChange={(event) => setDescription(event.target.value)}
					/>
					<button type="submit" className="rounded-md bg-gold px-3 py-2 text-sm font-medium text-ink sm:col-span-2">
						Add topic
					</button>
				</form>
			</section>
		</div>
	);
}
