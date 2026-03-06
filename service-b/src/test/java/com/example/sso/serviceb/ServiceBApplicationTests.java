package com.example.sso.serviceb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceBApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JwtDecoder jwtDecoder;

	@MockBean
	private com.example.sso.serviceb.config.JweTokenService jweTokenService;

	@BeforeEach
	void setupMockDecoder() {
		doAnswer(invocation -> invocation.getArgument(0)).when(jweTokenService).decryptToSignedJwt(anyString());

		doThrow(validationError("Invalid token")).when(jwtDecoder).decode(anyString());

		doReturn(jwtToken(
				"valid-token",
				"demo",
				"service.b.read",
				"service-b",
				"http://localhost:9000",
				Instant.now().plusSeconds(300)
		)).when(jwtDecoder).decode("valid-token");

		doReturn(jwtToken(
				"missing-scope-token",
				"demo",
				"service.a.read",
				"service-b",
				"http://localhost:9000",
				Instant.now().plusSeconds(300)
		)).when(jwtDecoder).decode("missing-scope-token");

		doThrow(validationError("Token is expired")).when(jwtDecoder).decode("expired-token");
		doThrow(validationError("Token issuer is invalid")).when(jwtDecoder).decode("invalid-issuer-token");
	}

	@Test
	void validScopeAllowsServiceBAccess() throws Exception {
		mockMvc.perform(get("/api/b/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk());
	}

	@Test
	void missingScopeReturnsForbidden() throws Exception {
		mockMvc.perform(get("/api/b/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer missing-scope-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void expiredTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/b/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidIssuerReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/b/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-issuer-token"))
				.andExpect(status().isUnauthorized());
	}

	private static Jwt jwtToken(
			String tokenValue,
			String subject,
			String scope,
			String audience,
			String issuer,
			Instant expiresAt
	) {
		Instant issuedAt = Instant.now().minusSeconds(5);
		return new Jwt(
				tokenValue,
				issuedAt,
				expiresAt,
				Map.of("alg", "RS256"),
				Map.of(
						JwtClaimNames.SUB, subject,
						JwtClaimNames.ISS, issuer,
						JwtClaimNames.AUD, List.of(audience),
						JwtClaimNames.IAT, issuedAt,
						JwtClaimNames.EXP, expiresAt,
						"scope", scope
				)
		);
	}

	private static JwtValidationException validationError(String message) {
		OAuth2Error error = new OAuth2Error("invalid_token", message, null);
		return new JwtValidationException(message, List.of(error));
	}
}
