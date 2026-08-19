package com.newspulse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.LoginRequest;
import com.newspulse.security.JwtService;
import com.newspulse.support.AppPropertiesFixture;
import com.newspulse.web.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

	private AuthService authService;
	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		AppProperties properties = AppPropertiesFixture.defaults();
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
