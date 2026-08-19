package com.newspulse.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Accepts Neon / Railway / Render {@code DATABASE_URL} values
 * ({@code postgres://user:pass@host/db}) and exposes them as Spring datasource properties.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String databaseUrl = environment.getProperty("DATABASE_URL");
		if (databaseUrl == null || databaseUrl.isBlank()) {
			return;
		}
		if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
			return;
		}
		if (environment.getProperty("SPRING_DATASOURCE_URL") != null
				|| environment.getProperty("spring.datasource.url") != null) {
			return;
		}

		URI uri = URI.create(databaseUrl.replaceFirst("^postgres://", "postgresql://"));
		String userInfo = uri.getUserInfo();
		if (userInfo == null || !userInfo.contains(":")) {
			throw new IllegalStateException("DATABASE_URL must include username and password");
		}
		int split = userInfo.indexOf(':');
		String username = urlDecode(userInfo.substring(0, split));
		String password = urlDecode(userInfo.substring(split + 1));
		int port = uri.getPort() > 0 ? uri.getPort() : 5432;
		String path = uri.getPath() == null || uri.getPath().isBlank() ? "/newspulse" : uri.getPath();
		String query = uri.getQuery() == null || uri.getQuery().isBlank() ? "sslmode=require" : uri.getQuery();
		String jdbcUrl = "jdbc:postgresql://%s:%d%s?%s".formatted(uri.getHost(), port, path, query);

		environment.getPropertySources().addFirst(new MapPropertySource("database-url", java.util.Map.of(
				"spring.datasource.url", jdbcUrl,
				"spring.datasource.username", username,
				"spring.datasource.password", password
		)));
	}

	private static String urlDecode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
