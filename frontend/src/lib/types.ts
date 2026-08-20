export type Sentiment = "POSITIVE" | "NEUTRAL" | "NEGATIVE";

export type Topic = {
	id: number;
	name: string;
	query: string;
	description: string | null;
	active: boolean;
	createdAt: string;
	updatedAt: string;
};

export type DigestItem = {
	rank: number;
	title: string;
	summary: string;
	sourceCount: number;
	sentiment: Sentiment | null;
	clusterId: number | null;
};

export type Digest = {
	id: number;
	topicId: number;
	topicName: string;
	digestDate: string;
	headline: string;
	overview: string;
	positivePct: number | string;
	neutralPct: number | string;
	negativePct: number | string;
	generatedAt: string;
	items: DigestItem[];
};

export type SentimentPoint = {
	date: string;
	positive: number;
	neutral: number;
	negative: number;
	positivePct: number | string;
	neutralPct: number | string;
	negativePct: number | string;
};

export type Stats = {
	topicId: number | null;
	from: string;
	to: string;
	articleCount: number;
	series: SentimentPoint[];
};

export type Article = {
	id: number;
	topicId: number;
	topicName: string;
	clusterId: number | null;
	title: string;
	url: string;
	source: string;
	sourceName: string;
	publishedAt: string | null;
	summary: string | null;
	sentiment: Sentiment | null;
	sentimentJustification: string | null;
	stanceTag: string | null;
};

export type PageResponse<T> = {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	last: boolean;
};

export type LoginResponse = {
	accessToken: string;
	tokenType: string;
	expiresInMs: number;
};

export type BatchSummary = {
	headline: string;
	overview: string;
	themes: string[];
	articleCount: number;
	articles: Array<{ id: number; title: string; sourceName: string }>;
	model: string;
};

export type PipelineMessage = {
	message: string;
	detail?: string;
};
