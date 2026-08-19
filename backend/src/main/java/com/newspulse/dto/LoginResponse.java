package com.newspulse.dto;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresInMs
) {}
