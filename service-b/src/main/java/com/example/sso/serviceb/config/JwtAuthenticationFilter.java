package com.example.sso.serviceb.config;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtDecoder jwtDecoder;
	private final JweTokenService jweTokenService;
	private final OpaqueIntrospectionClient opaqueIntrospectionClient;
	private final TokenMode tokenMode;
	private final String expectedAudience;

	public JwtAuthenticationFilter(
			JwtDecoder jwtDecoder,
			JweTokenService jweTokenService,
			OpaqueIntrospectionClient opaqueIntrospectionClient,
			@Value("${service-b.token.mode:jwe}") String tokenModeRaw,
			@Value("${service-b.audience}") String expectedAudience
	) {
		this.jwtDecoder = jwtDecoder;
		this.jweTokenService = jweTokenService;
		this.opaqueIntrospectionClient = opaqueIntrospectionClient;
		this.tokenMode = TokenMode.from(tokenModeRaw);
		this.expectedAudience = expectedAudience;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return path.startsWith("/actuator/")
				|| path.equals("/swagger-ui.html")
				|| path.startsWith("/swagger-ui/")
				|| path.startsWith("/v3/api-docs/")
				|| path.equals("/error")
				|| path.equals("/favicon.ico");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String tokenValue = authorizationHeader.substring("Bearer ".length()).trim();
		try {
			UsernamePasswordAuthenticationToken authentication = authenticate(tokenValue);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"invalid_token\",\"message\":\"" + escapeJson(ex.getMessage()) + "\"}");
		}
	}

	private UsernamePasswordAuthenticationToken authenticate(String tokenValue) {
		if (tokenMode == TokenMode.JWE) {
			String signedJwt = jweTokenService.decryptToSignedJwt(tokenValue);
			var jwt = jwtDecoder.decode(signedJwt);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					jwt.getSubject(),
					tokenValue,
					extractAuthorities(jwt.getClaims())
			);
			authentication.setDetails(jwt.getClaims());
			return authentication;
		}

		if (tokenMode == TokenMode.OPAQUE) {
			OpaqueIntrospectionClient.OpaqueIntrospectionResult introspection = opaqueIntrospectionClient.introspect(tokenValue);
			if (!introspection.active()) {
				throw new IllegalStateException("Token is inactive");
			}
			if (!introspection.audience().contains(expectedAudience)) {
				throw new IllegalStateException("Token audience is not valid");
			}

			Map<String, Object> claims = new LinkedHashMap<>();
			claims.put("sub", introspection.subject());
			claims.put("scope", introspection.scopeAsString());
			claims.put("aud", introspection.audience());
			claims.put("iss", introspection.issuer());
			if (introspection.tokenId() != null) {
				claims.put("jti", introspection.tokenId());
			}

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					introspection.subject(),
					tokenValue,
					extractAuthorities(claims)
			);
			authentication.setDetails(claims);
			return authentication;
		}

		throw new IllegalStateException("Unsupported token mode: " + tokenMode);
	}

	private Collection<? extends GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
		Set<String> scopes = new LinkedHashSet<>();
		Object scopeClaim = claims.get("scope");
		if (scopeClaim instanceof String scopeString) {
			for (String scope : scopeString.split("\\s+")) {
				String normalized = scope.trim();
				if (!normalized.isEmpty()) {
					scopes.add(normalized);
				}
			}
		}

		Object scpClaim = claims.get("scp");
		if (scpClaim instanceof Collection<?> scpCollection) {
			for (Object value : scpCollection) {
				if (value != null) {
					String normalized = value.toString().trim();
					if (!normalized.isEmpty()) {
						scopes.add(normalized);
					}
				}
			}
		}

		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		for (String scope : scopes) {
			authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
		}
		return authorities;
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
