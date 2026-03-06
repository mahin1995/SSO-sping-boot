package com.example.sso.serviceb.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpaqueIntrospectionClient {

	private final RestClient restClient;
	private final String introspectionUrl;
	private final String introspectionSecret;

	public OpaqueIntrospectionClient(
			RestClient.Builder restClientBuilder,
			@Value("${service-b.introspection.url:http://localhost:9000/api/auth/introspect}") String introspectionUrl,
			@Value("${service-b.introspection.secret:change-me-introspection-secret}") String introspectionSecret
	) {
		this.restClient = restClientBuilder.build();
		this.introspectionUrl = introspectionUrl;
		this.introspectionSecret = introspectionSecret;
	}

	public OpaqueIntrospectionResult introspect(String tokenValue) {
		try {
			IntrospectionApiResponse response = restClient.post()
					.uri(introspectionUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.header("X-Introspection-Secret", introspectionSecret)
					.body(new IntrospectionRequest(tokenValue))
					.retrieve()
					.body(IntrospectionApiResponse.class);

			if (response == null || response.data() == null) {
				return OpaqueIntrospectionResult.inactive();
			}
			return OpaqueIntrospectionResult.from(response.data());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Opaque introspection request failed", ex);
		}
	}

	private record IntrospectionRequest(String token) {
	}

	private record IntrospectionApiResponse(IntrospectionData data) {
	}

	private record IntrospectionData(
			boolean active,
			String sub,
			String scope,
			List<String> audience,
			Long exp,
			Long iat,
			String iss,
			String jti
	) {
	}

	public record OpaqueIntrospectionResult(
			boolean active,
			String subject,
			Set<String> scopes,
			List<String> audience,
			String issuer,
			String tokenId
	) {
		static OpaqueIntrospectionResult inactive() {
			return new OpaqueIntrospectionResult(false, null, Set.of(), List.of(), null, null);
		}

		static OpaqueIntrospectionResult from(IntrospectionData data) {
			if (data == null || !data.active()) {
				return inactive();
			}

			Set<String> scopes = new LinkedHashSet<>();
			if (data.scope() != null) {
				for (String scope : data.scope().split("\\s+")) {
					String normalized = scope.trim();
					if (!normalized.isEmpty()) {
						scopes.add(normalized);
					}
				}
			}

			List<String> audience = data.audience() == null ? List.of() : data.audience();
			return new OpaqueIntrospectionResult(
					true,
					data.sub(),
					scopes,
					audience,
					data.iss(),
					data.jti()
			);
		}

		String scopeAsString() {
			return String.join(" ", scopes);
		}
	}
}
