package com.example.sso.serviceb.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/b/secure").hasAuthority("SCOPE_service.b.read")
						.anyRequest().authenticated())
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${service-b.jwt.issuer:http://localhost:9000}") String issuer,
			@Value("${service-b.jwt.jwk-set-uri:http://localhost:9000/.well-known/jwks.json}") String jwkSetUri,
			@Value("${service-b.audience}") String expectedAudience,
			@Value("${service-b.allowed-jws-algorithm:RS256}") String requiredJwsAlgorithm,
			JwksPayloadCryptoService jwksPayloadCryptoService,
			ObjectMapper objectMapper
	) {
		URL jwksUrl;
		try {
			jwksUrl = new URL(jwkSetUri);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid service-b.jwt.jwk-set-uri: " + jwkSetUri, ex);
		}

		ResourceRetriever delegateRetriever = new DefaultResourceRetriever(2_000, 2_000, 1_048_576);
		ResourceRetriever encryptedRetriever = new EncryptedJwksResourceRetriever(
				delegateRetriever,
				jwksPayloadCryptoService,
				objectMapper
		);

		JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(jwksUrl, encryptedRetriever);
		ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
				JWSAlgorithm.parse(requiredJwsAlgorithm),
				jwkSource
		);
		jwtProcessor.setJWSKeySelector(keySelector);

		NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(jwtProcessor);
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer),
				new AudienceValidator(expectedAudience),
				new AlgorithmValidator(requiredJwsAlgorithm),
				new JwtIdValidator()
		);
		jwtDecoder.setJwtValidator(validator);
		return jwtDecoder;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://127.0.0.1:8085,http://localhost:8085}") List<String> allowedOrigins
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
