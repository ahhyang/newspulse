import { describe, expect, it } from "vitest";
import { buildQuery, formatPct, shiftDay } from "./format";

describe("formatPct", () => {
	it("renders integers without decimals", () => {
		expect(formatPct(100)).toBe("100%");
		expect(formatPct("0.00")).toBe("0%");
	});

	it("keeps two-decimal sentiment mixes", () => {
		expect(formatPct("66.67")).toBe("66.67%");
	});
});

describe("shiftDay", () => {
	it("moves across month boundaries in UTC", () => {
		expect(shiftDay("2026-08-01", -1)).toBe("2026-07-31");
		expect(shiftDay("2026-08-18", 1)).toBe("2026-08-19");
	});
});

describe("buildQuery", () => {
	it("omits empty values", () => {
		expect(buildQuery({ topicId: 1, sourceName: "", sentiment: undefined })).toBe("?topicId=1");
	});
});
