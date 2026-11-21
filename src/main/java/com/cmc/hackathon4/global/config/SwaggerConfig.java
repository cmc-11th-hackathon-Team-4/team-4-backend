package com.cmc.hackathon4.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI 주소: /swagger-ui.html
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI().info(new Info()
				.title("CMC 20th 해커톤 4조 API")
				.description("모든 시간 값은 UTC ISO-8601 형식입니다. 예: 2026-11-21T05:30:00Z")
				.version("v1"));
	}

}
