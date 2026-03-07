package com.example.sso.serviceb.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI serviceBOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Service B API")
						.version("v1")
						.description("SAML-protected Service B endpoint"))
				.components(new Components());
	}
}

