import { getToken, setToken } from "./auth";
import { buildQuery } from "./format";
import type {
	Article,
	Digest,
	LoginResponse,
	PageResponse,
	Stats,
	Topic,
	BatchSummary,
} from "./types";

const API_BASE = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");

export class ApiRequestError extends Error {
	status: number;
	constructor(status: number, message: string) {
		super(message);
		this.name = "ApiRequestError";
		this.status = status;
	}
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
	const headers = new Headers(init.headers);
	if (init.body && !headers.has("Content-Type")) {
		headers.set("Content-Type", "application/json");
	}
	const token = getToken();
	if (token) {
		headers.set("Authorization", `Bearer ${token}`);
	}
	const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
	const text = await response.text();
	const payload = text ? (JSON.parse(text) as { message?: string }) : null;
	if (!response.ok) {
		if (response.status === 401) {
			setToken(null);
		}
		throw new ApiRequestError(response.status, payload?.message ?? `Request failed (${response.status})`);
	}
	return payload as T;
}

export const api = {
	topics: () => request<Topic[]>("/api/topics"),
	digestLatest: (topicId?: number) => request<Digest>(`/api/digests/latest${buildQuery({ topicId })}`),
	digestByDate: (date: string, topicId?: number) =>
		request<Digest>(`/api/digests/${date}${buildQuery({ topicId })}`),
	stats: (topicId?: number, from?: string, to?: string) =>
		request<Stats>(`/api/stats${buildQuery({ topicId, from, to })}`),
	articles: (params: Record<string, string | number | undefined>) =>
		request<PageResponse<Article>>(`/api/articles${buildQuery(params)}`),
	article: (id: number) => request<Article>(`/api/articles/${id}`),
	login: (username: string, password: string) =>
		request<LoginResponse>("/api/auth/login", {
			method: "POST",
			body: JSON.stringify({ username, password }),
		}),
	createTopic: (body: { name: string; query: string; description?: string }) =>
		request<Topic>("/api/topics", { method: "POST", body: JSON.stringify(body) }),
	updateTopic: (id: number, body: { active?: boolean; name?: string; query?: string; description?: string }) =>
		request<Topic>(`/api/topics/${id}`, { method: "PATCH", body: JSON.stringify(body) }),
	ingest: () => request<Record<string, unknown>>("/api/ingestion/runs", { method: "POST" }),
	enrich: () => request<Record<string, unknown>>("/api/enrichment/runs", { method: "POST" }),
	generateDigest: (date?: string, topicId?: number) =>
		request<Record<string, unknown>>(`/api/digests/runs${buildQuery({ date, topicId })}`, { method: "POST" }),
	summarizeBatch: (articleIds: number[]) =>
		request<BatchSummary>("/api/summaries/batch", {
			method: "POST",
			body: JSON.stringify({ articleIds }),
		}),
};
