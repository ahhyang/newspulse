package com.newspulse.service;

import com.newspulse.config.AppProperties;
import com.newspulse.dto.LoginRequest;
import com.newspulse.dto.LoginResponse;
import com.newspulse.security.JwtService;
import com.newspulse.web.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AppProperties appProperties;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final String adminPasswordHash;

	public AuthService(AppProperties appProperties, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.appProperties = appProperties;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.adminPasswordHash = passwordEncoder.encode(appProperties.admin().password());
	}

	public LoginResponse login(LoginRequest request) {
		boolean usernameMatches = appProperties.admin().username().equals(request.username());
		boolean passwordMatches = passwordEncoder.matches(request.password(), adminPasswordHash);
		if (!usernameMatches || !passwordMatches) {
			throw new UnauthorizedException("Invalid username or password");
		}
		String token = jwtService.generateToken(request.username());
		return new LoginResponse(token, "Bearer", appProperties.jwt().expirationMs());
	}
}
