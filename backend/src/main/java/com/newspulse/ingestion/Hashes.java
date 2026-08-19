package com.newspulse.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashes {

	private Hashes() {}

	public static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required", ex);
		}
	}

	public static String contentHash(String rawContent) {
		if (rawContent == null) {
			return sha256("");
		}
		String collapsed = rawContent.trim().replaceAll("\\s+", " ");
		return sha256(collapsed);
	}
}
