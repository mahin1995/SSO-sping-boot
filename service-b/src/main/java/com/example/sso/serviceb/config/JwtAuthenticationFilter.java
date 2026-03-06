package com.example.sso.serviceb.config;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtDecoder jwtDecoder;

	public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
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
			Jwt jwt = jwtDecoder.decode(tokenValue);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					jwt.getSubject(),
					tokenValue,
					extractAuthorities(jwt)
			);
			authentication.setDetails(jwt.getClaims());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		}
		catch (JwtException ex) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"invalid_token\",\"message\":\"" + escapeJson(ex.getMessage()) + "\"}");
		}
	}

	private Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt) {
		Set<String> scopes = new LinkedHashSet<>();
		Object scopeClaim = jwt.getClaims().get("scope");
		if (scopeClaim instanceof String scopeString) {
			for (String scope : scopeString.split("\\s+")) {
				String normalized = scope.trim();
				if (!normalized.isEmpty()) {
					scopes.add(normalized);
				}
			}
		}

		Object scpClaim = jwt.getClaims().get("scp");
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
