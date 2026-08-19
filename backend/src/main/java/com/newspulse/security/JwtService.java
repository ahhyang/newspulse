package com.newspulse.security;

import com.newspulse.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final AppProperties appProperties;

	public JwtService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public String generateToken(String username) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(appProperties.jwt().expirationMs());
		return Jwts.builder()
				.subject(username)
				.claim("role", "ADMIN")
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(signingKey())
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey signingKey() {
		byte[] keyBytes = appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
