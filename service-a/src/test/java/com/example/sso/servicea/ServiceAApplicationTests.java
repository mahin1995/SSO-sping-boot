package com.example.sso.servicea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.example.sso.servicea.config.JwksPayloadCryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceAApplicationTests {

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:h2:mem:service_a;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
		registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
		registry.add("spring.datasource.username", () -> "sa");
		registry.add("spring.datasource.password", () -> "");
		registry.add("service-a.seed.demo-user", () -> "demo");
		registry.add("service-a.seed.demo-password", () -> "demo1234");
		registry.add("service-a.issuer", () -> "http://localhost:9000");
		registry.add("service-a.service-client.id", () -> "internal-client");
		registry.add("service-a.service-client.secret", () -> "internal-secret");
		registry.add("service-a.service-client.scopes", () -> "service.a.read,service.b.read");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwksPayloadCryptoService jwksPayloadCryptoService;

	@Test
	void flywayMigrationsAreAppliedForRoleBasedUserTables() {
		Integer appUsersTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'app_users'",
				Integer.class
		);
		Integer appRolesTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'app_roles'",
				Integer.class
		);
		Integer appScopesTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'app_scopes'",
				Integer.class
		);
		Integer appUserRolesTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'app_user_roles'",
				Integer.class
		);
		Integer appRoleScopesTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'app_role_scopes'",
				Integer.class
		);
		Integer opaqueTokensTableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_name = 'opaque_access_tokens'",
				Integer.class
		);

		assertThat(appUsersTableCount).isGreaterThanOrEqualTo(1);
		assertThat(appRolesTableCount).isGreaterThanOrEqualTo(1);
		assertThat(appScopesTableCount).isGreaterThanOrEqualTo(1);
		assertThat(appUserRolesTableCount).isGreaterThanOrEqualTo(1);
		assertThat(appRoleScopesTableCount).isGreaterThanOrEqualTo(1);
		assertThat(opaqueTokensTableCount).isGreaterThanOrEqualTo(1);
	}

	@Test
	void jwksEndpointExposesEncryptedPublicRsaKeyPayload() throws Exception {
		MvcResult result = mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.format").value("aes-gcm+base64"))
				.andExpect(jsonPath("$.payload").isString())
				.andReturn();

		String encryptedPayload = objectMapper.readTree(result.getResponse().getContentAsString())
				.path("payload")
				.asText();

		String decryptedJwks = jwksPayloadCryptoService.decryptFromBase64(encryptedPayload);
		JsonNode root = objectMapper.readTree(decryptedJwks);
		JsonNode firstKey = root.path("keys").get(0);

		assertThat(firstKey).isNotNull();
		assertThat(firstKey.path("kty").asText()).isEqualTo("RSA");
		assertThat(firstKey.path("kid").asText()).isNotBlank();
	}

	@Test
	void registerAndLoginApiIssueTokenAndAllowAccessToServiceA() throws Exception {
		Map<String, Object> registerRequest = Map.of(
				"username", "new-user",
				"password", "new-password",
				"roles", List.of("APP_USER")
		);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(registerRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.username").value("new-user"));

		Map<String, Object> loginRequest = Map.of(
				"username", "new-user",
				"password", "new-password"
		);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data")
				.path("accessToken")
				.asText();

		mockMvc.perform(get("/api/a/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.service").value("service-a"));
	}

	@Test
	void serviceClientTokenAllowsServiceAAccess() throws Exception {
		Map<String, Object> serviceTokenRequest = Map.of(
				"clientId", "internal-client",
				"clientSecret", "internal-secret"
		);

		MvcResult tokenResult = mockMvc.perform(post("/api/auth/service-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(serviceTokenRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andExpect(jsonPath("$.data.scope").value(org.hamcrest.Matchers.containsString("service.a.read")))
				.andReturn();

		String accessToken = objectMapper.readTree(tokenResult.getResponse().getContentAsString())
				.path("data")
				.path("accessToken")
				.asText();

		mockMvc.perform(get("/api/a/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.service").value("service-a"));
	}

	@Test
	void invalidServiceClientCredentialsAreRejected() throws Exception {
		Map<String, Object> serviceTokenRequest = Map.of(
				"clientId", "internal-client",
				"clientSecret", "wrong-secret"
		);

		mockMvc.perform(post("/api/auth/service-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(serviceTokenRequest)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void roleBasedScopesCanBeManagedAndAffectServiceAAuthorization() throws Exception {
		Map<String, Object> registerRequest = Map.of(
				"username", "rbac-user",
				"password", "rbac-password",
				"roles", List.of("APP_USER")
		);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(registerRequest)))
				.andExpect(status().isCreated());

		Map<String, Object> loginRequest = Map.of(
				"username", "rbac-user",
				"password", "rbac-password"
		);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
				.path("data")
				.path("accessToken")
				.asText();

		Map<String, Object> roleRequest = Map.of(
				"roleName", "ONLY_B",
				"description", "Service B only",
				"scopes", List.of("service.b.read")
		);

		mockMvc.perform(post("/api/admin/roles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(roleRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.roleName").value("ONLY_B"));

		Map<String, Object> assignRolesRequest = Map.of(
				"roles", List.of("ONLY_B")
		);

		mockMvc.perform(post("/api/admin/users/rbac-user/roles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(assignRolesRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.scopes[0]").value("service.b.read"));

		MvcResult reloginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk())
				.andReturn();

		String tokenAfterRoleUpdate = objectMapper.readTree(reloginResult.getResponse().getContentAsString())
				.path("data")
				.path("accessToken")
				.asText();

		mockMvc.perform(get("/api/a/secure")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAfterRoleUpdate))
				.andExpect(status().isUnauthorized());
	}
}
