package com.newspulse.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads a gitignored {@code .env} file for local development without overriding real env vars.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Path envFile = resolveEnvFile();
		if (envFile == null) {
			return;
		}
		Map<String, Object> values = parse(envFile);
		if (!values.isEmpty()) {
			environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
		}
	}

	private static Path resolveEnvFile() {
		List<Path> candidates = List.of(
				Path.of(".env"),
				Path.of("../.env"),
				Path.of("backend/.env")
		);
		return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(null);
	}

	private static Map<String, Object> parse(Path envFile) {
		Map<String, Object> values = new LinkedHashMap<>();
		try {
			for (String raw : Files.readAllLines(envFile)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
					continue;
				}
				int eq = line.indexOf('=');
				String key = line.substring(0, eq).trim();
				String value = line.substring(eq + 1).trim();
				if ((value.startsWith("\"") && value.endsWith("\""))
						|| (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}
				values.put(key, value);
			}
		} catch (IOException ignored) {
			return Map.of();
		}
		return values;
	}
}
