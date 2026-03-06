package com.example.sso.serviceb.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecureBService {

	public Map<String, Object> securePayload(Authentication authentication) {
		Set<String> scopes = authentication.getAuthorities().stream()
				.map(grantedAuthority -> grantedAuthority.getAuthority())
				.filter(authority -> authority.startsWith("SCOPE_"))
				.map(authority -> authority.substring("SCOPE_".length()))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("service", "service-b");
		payload.put("subject", authentication.getName());
		payload.put("scopes", scopes);
		payload.put("timestamp", Instant.now().toString());
		return payload;
	}
}

