package com.example.sso.servicea.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	@Bean
	@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
		RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

		http
				.securityMatcher(endpointsMatcher)
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
				.with(authorizationServerConfigurer, configurer -> configurer.oidc(Customizer.withDefaults()));

		return http.build();
	}

	@Bean
	@org.springframework.core.annotation.Order(2)
	SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers("/api/admin/**").hasAuthority("SCOPE_service.a.read")
						.requestMatchers("/api/a/secure").hasAuthority("SCOPE_service.a.read")
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/**", "/api/admin/**"))
				.formLogin(Customizer.withDefaults())
				.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings(@Value("${service-a.issuer}") String issuer) {
		return AuthorizationServerSettings.builder().issuer(issuer).build();
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcRegisteredClientRepository(jdbcTemplate);
	}

	@Bean
	OAuth2AuthorizationService authorizationService(
			JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository
	) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	OAuth2AuthorizationConsentService authorizationConsentService(
			JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository
	) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JWKSource<SecurityContext> jwkSource(Environment environment) {
		RsaKeyPair rsaKeyPair = resolveRsaKeyPair(environment);
		String keyId = environment.getProperty("service-a.keys.key-id", "service-a-key");
		RSAKey rsaKey = new RSAKey.Builder(rsaKeyPair.publicKey())
				.privateKey(rsaKeyPair.privateKey())
				.keyID(keyId)
				.build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	@Bean
	JwtDecoder jwtDecoder(
			JWKSource<SecurityContext> jwkSource,
			@Value("${service-a.issuer}") String issuer,
			@Value("${service-a.audiences.service-a}") String serviceAAudience
	) {
		NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuer),
				new AudienceValidator(serviceAAudience)
		);
		jwtDecoder.setJwtValidator(validator);
		return jwtDecoder;
	}

	@Bean
	JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
			@Value("${service-a.audiences.service-a}") String serviceAAudience,
			@Value("${service-a.audiences.service-b}") String serviceBAudience
	) {
		return context -> {
			if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				return;
			}

			Set<String> audiences = new LinkedHashSet<>();
			Set<String> scopes = context.getAuthorizedScopes();
			if (scopes.contains("service.a.read")) {
				audiences.add(serviceAAudience);
			}
			if (scopes.contains("service.b.read")) {
				audiences.add(serviceBAudience);
			}
			if (!audiences.isEmpty()) {
				context.getClaims().audience(new ArrayList<>(audiences));
			}
		};
	}

	@Bean
	TokenSettings defaultTokenSettings() {
		return TokenSettings.builder()
				.accessTokenTimeToLive(Duration.ofMinutes(10))
				.refreshTokenTimeToLive(Duration.ofHours(8))
				.reuseRefreshTokens(false)
				.build();
	}

	@Bean
	ClientSettings defaultClientSettings() {
		return ClientSettings.builder().requireAuthorizationConsent(false).build();
	}

	private RsaKeyPair resolveRsaKeyPair(Environment environment) {
		Optional<RsaKeyPair> keyPairFromPemProperties = loadKeysFromRawPem(environment);
		if (keyPairFromPemProperties.isPresent()) {
			return keyPairFromPemProperties.get();
		}

		Optional<RsaKeyPair> keyPairFromFiles = loadKeysFromFiles(environment);
		if (keyPairFromFiles.isPresent()) {
			return keyPairFromFiles.get();
		}

		log.warn("No RSA keys configured for service-a. Generating ephemeral keys for this runtime.");
		return generateEphemeralKeyPair();
	}

	private Optional<RsaKeyPair> loadKeysFromRawPem(Environment environment) {
		String privatePem = environment.getProperty("service-a.keys.private-key-pem");
		String publicPem = environment.getProperty("service-a.keys.public-key-pem");
		if (isBlank(privatePem) || isBlank(publicPem)) {
			return Optional.empty();
		}
		return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
	}

	private Optional<RsaKeyPair> loadKeysFromFiles(Environment environment) {
		String privatePath = environment.getProperty("service-a.keys.private-key-path");
		String publicPath = environment.getProperty("service-a.keys.public-key-path");
		if (isBlank(privatePath) || isBlank(publicPath)) {
			return Optional.empty();
		}
		try {
			String privatePem = Files.readString(Path.of(privatePath));
			String publicPem = Files.readString(Path.of(publicPath));
			return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load RSA key files for service-a", ex);
		}
	}

	private RsaKeyPair generateEphemeralKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			return new RsaKeyPair((RSAPrivateKey) keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to generate ephemeral RSA key pair", ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
	}
}
