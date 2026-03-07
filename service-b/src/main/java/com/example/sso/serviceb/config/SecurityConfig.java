package com.example.sso.serviceb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/",
								"/actuator/health",
								"/actuator/info",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs",
								"/v3/api-docs/**"
						).permitAll()
						.requestMatchers("/api/b/**").authenticated()
						.anyRequest().authenticated())
				.saml2Login(Customizer.withDefaults())
				.saml2Logout(Customizer.withDefaults())
				.saml2Metadata(Customizer.withDefaults())
				.logout(logout -> logout.logoutSuccessUrl("/"))
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

		return http.build();
	}
}
