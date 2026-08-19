package com.newspulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures Jackson 3 ({@code JsonMapper}) for HTTP.
 * Application code still uses Jackson 2 ({@code com.fasterxml.jackson.databind.ObjectMapper}).
 */
@Configuration
public class Jackson2Config {

	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}
}
