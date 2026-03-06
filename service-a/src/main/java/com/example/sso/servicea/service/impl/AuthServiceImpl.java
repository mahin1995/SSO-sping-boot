package com.example.sso.servicea.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

	private final AppUserAccountService appUserAccountService;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtEncoder jwtEncoder;
	private final String issuer;
	private final String serviceAAudience;
	private final String serviceBAudience;
	private final Duration customLoginTokenTtl;

	public AuthServiceImpl(
			AppUserAccountService appUserAccountService,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtEncoder jwtEncoder,
			@Value("${service-a.issuer}") String issuer,
			@Value("${service-a.audiences.service-a}") String serviceAAudience,
			@Value("${service-a.audiences.service-b}") String serviceBAudience,
			@Value("${service-a.custom-login.token-ttl-minutes:10}") long tokenTtlMinutes
	) {
		this.appUserAccountService = appUserAccountService;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtEncoder = jwtEncoder;
		this.issuer = issuer;
		this.serviceAAudience = serviceAAudience;
		this.serviceBAudience = serviceBAudience;
		this.customLoginTokenTtl = Duration.ofMinutes(tokenTtlMinutes);
	}

	@Override
	public RegisteredUser register(RegisterUserCommand command) {
		String username = normalizeRequired(command.username(), "username");
		String password = normalizeRequired(command.password(), "password");
		Set<String> roles = normalizeRoles(command.roles());

		if (appUserAccountService.existsByUsername(username)) {
			throw new UserAlreadyExistsException("Username already exists: " + username);
		}

		try {
			appUserAccountService.createUser(username, passwordEncoder.encode(password), roles);
		}
		catch (IllegalArgumentException | AppUserAccountService.RoleNotFoundException ex) {
			throw new InvalidRegistrationException(ex.getMessage());
		}

		AppUserAccountService.UserRoleScopeView userRoleScopeView = appUserAccountService.getUserRoleScopeView(username);
		return new RegisteredUser(username, userRoleScopeView.roles(), userRoleScopeView.scopes());
	}

	@Override
	public LoginToken login(LoginCommand command) {
		String username = normalizeRequired(command.username(), "username");
		String password = normalizeRequired(command.password(), "password");

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(username, password)
			);
		}
		catch (AuthenticationException ex) {
			throw new InvalidCredentialsException("Invalid username or password");
		}

		Set<String> scopes = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("SCOPE_"))
				.map(authority -> authority.substring("SCOPE_".length()))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(customLoginTokenTtl);
		List<String> audiences = resolveAudiences(scopes);

		JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(authentication.getName())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("scope", String.join(" ", scopes));

		if (!audiences.isEmpty()) {
			claimsBuilder.audience(audiences);
		}

		String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(
						JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(),
						claimsBuilder.build()
				))
				.getTokenValue();

		return new LoginToken(
				tokenValue,
				"Bearer",
				customLoginTokenTtl.getSeconds(),
				String.join(" ", scopes),
				audiences
		);
	}

	private Set<String> normalizeRoles(Set<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return new LinkedHashSet<>(Set.of("APP_USER"));
		}

		Set<String> normalized = roles.stream()
				.map(role -> role == null ? "" : role.trim())
				.filter(role -> !role.isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (normalized.isEmpty()) {
			throw new InvalidRegistrationException("At least one valid role is required");
		}
		return normalized;
	}

	private List<String> resolveAudiences(Set<String> scopes) {
		Set<String> audiences = new LinkedHashSet<>();
		if (scopes.contains("service.a.read")) {
			audiences.add(serviceAAudience);
		}
		if (scopes.contains("service.b.read")) {
			audiences.add(serviceBAudience);
		}
		return new ArrayList<>(audiences);
	}

	private String normalizeRequired(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new InvalidRegistrationException(fieldName + " is required");
		}
		return value.trim();
	}
}
