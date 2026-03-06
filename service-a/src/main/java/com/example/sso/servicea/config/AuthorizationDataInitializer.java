package com.example.sso.servicea.config;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.service.RoleScopeService;

@Component
public class AuthorizationDataInitializer implements ApplicationRunner {

	private final RegisteredClientRepository registeredClientRepository;
	private final AppUserAccountService appUserAccountService;
	private final RoleScopeService roleScopeService;
	private final PasswordEncoder passwordEncoder;

	@Value("${service-a.seed.pkce-client-id}")
	private String pkceClientId;

	@Value("${service-a.seed.pkce-redirect-uri}")
	private String pkceRedirectUri;

	@Value("${service-a.seed.internal-client-id}")
	private String internalClientId;

	@Value("${service-a.seed.internal-client-secret}")
	private String internalClientSecret;

	@Value("${service-a.seed.demo-user}")
	private String demoUser;

	@Value("${service-a.seed.demo-password}")
	private String demoPassword;

	public AuthorizationDataInitializer(
			RegisteredClientRepository registeredClientRepository,
			AppUserAccountService appUserAccountService,
			RoleScopeService roleScopeService,
			PasswordEncoder passwordEncoder
	) {
		this.registeredClientRepository = registeredClientRepository;
		this.appUserAccountService = appUserAccountService;
		this.roleScopeService = roleScopeService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedPkceClient();
		seedInternalClient();
		seedRolesAndScopes();
		seedDemoUser();
	}

	private void seedPkceClient() {
		if (registeredClientRepository.findByClientId(pkceClientId) != null) {
			return;
		}

		RegisteredClient pkceClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(pkceClientId)
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri(pkceRedirectUri)
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.scope("service.a.read")
				.scope("service.b.read")
				.clientSettings(ClientSettings.builder()
						.requireProofKey(true)
						.requireAuthorizationConsent(false)
						.build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(10))
						.refreshTokenTimeToLive(Duration.ofHours(8))
						.reuseRefreshTokens(false)
						.build())
				.build();

		registeredClientRepository.save(pkceClient);
	}

	private void seedInternalClient() {
		if (registeredClientRepository.findByClientId(internalClientId) != null) {
			return;
		}

		RegisteredClient internalClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(internalClientId)
				.clientSecret(passwordEncoder.encode(internalClientSecret))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("service.a.read")
				.scope("service.b.read")
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(Duration.ofMinutes(10))
						.reuseRefreshTokens(false)
						.build())
				.build();

		registeredClientRepository.save(internalClient);
	}

	private void seedDemoUser() {
		appUserAccountService.createUserIfMissing(
				demoUser,
				passwordEncoder.encode(demoPassword),
				Set.of("APP_USER")
		);
	}

	private void seedRolesAndScopes() {
		roleScopeService.createOrUpdateRole(
				"APP_USER",
				"Default application user role",
				Set.of("service.a.read", "service.b.read")
		);
		roleScopeService.createOrUpdateRole(
				"SERVICE_A_READER",
				"Can read Service A endpoint",
				Set.of("service.a.read")
		);
		roleScopeService.createOrUpdateRole(
				"SERVICE_B_READER",
				"Can read Service B endpoint",
				Set.of("service.b.read")
		);
	}
}
