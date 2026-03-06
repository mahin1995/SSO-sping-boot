package com.example.sso.servicea.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface OpaqueTokenService {

	String issueToken(
			String subject,
			Set<String> scopes,
			List<String> audience,
			Instant issuedAt,
			Instant expiresAt,
			String issuer,
			String tokenId
	);

	OpaqueTokenIntrospection introspect(String tokenValue);

	record OpaqueTokenIntrospection(
			boolean active,
			String subject,
			Set<String> scopes,
			List<String> audience,
			Instant issuedAt,
			Instant expiresAt,
			String issuer,
			String tokenId
	) {
		public static OpaqueTokenIntrospection inactive() {
			return new OpaqueTokenIntrospection(false, null, Set.of(), List.of(), null, null, null, null);
		}

		public String scopeAsString() {
			return String.join(" ", scopes);
		}
	}
}
