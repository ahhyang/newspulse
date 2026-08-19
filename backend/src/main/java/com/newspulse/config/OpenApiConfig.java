package com.newspulse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI newsPulseOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("NewsPulse API")
						.version("0.1.0")
						.description("AI-native news aggregation and sentiment briefing API.")
						.contact(new Contact().name("NewsPulse").url("https://github.com/ahhyang/newspulse"))
						.license(new License().name("MIT")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("Local"),
						new Server().url("/").description("Current host")
				))
				.components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")));
	}
}
