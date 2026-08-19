package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.LoginRequest;
import com.newspulse.security.JwtService;
import com.newspulse.web.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

	private AuthService authService;
	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties(
				new AppProperties.Jwt("test-jwt-secret-key-that-is-long-enough-for-hs256-algorithms", 3600000),
				new AppProperties.Admin("admin", "s3cret"),
				new AppProperties.Cors("http://localhost:5173"),
				new AppProperties.Ingestion(false, 3600000, 20000, 24),
				new AppProperties.Llm("openrouter", "https://openrouter.ai/api/v1", "test", "model", "https://example.com", "NewsPulse"),
				new AppProperties.Gnews(true, "test", "https://gnews.io/api/v4", "en", 10)
		);
		jwtService = new JwtService(properties);
		authService = new AuthService(properties, new BCryptPasswordEncoder(), jwtService);
	}

	@Test
	void loginReturnsSignedJwtForValidCredentials() {
		var response = authService.login(new LoginRequest("admin", "s3cret"));

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.accessToken()).isNotBlank();
		assertThat(jwtService.isValid(response.accessToken())).isTrue();
		assertThat(jwtService.extractUsername(response.accessToken())).isEqualTo("admin");
	}

	@Test
	void loginRejectsBadPassword() {
		assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "nope")))
				.isInstanceOf(UnauthorizedException.class);
	}
}
