package com.example.sso.servicea.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

@Service
public class SecureAService {

	public Map<String, Object> securePayload(Authentication authentication) {
		Set<String> authorities = authentication.getAuthorities().stream()
				.map(grantedAuthority -> grantedAuthority.getAuthority())
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("service", "service-a");
		payload.put("subject", authentication.getName());
		payload.put("authorities", authorities);
		Object principal = authentication.getPrincipal();
		if (principal instanceof Saml2AuthenticatedPrincipal samlPrincipal) {
			payload.put("registrationId", samlPrincipal.getRelyingPartyRegistrationId());
			payload.put("attributeKeys", samlPrincipal.getAttributes().keySet());
		}
		payload.put("timestamp", Instant.now().toString());
		return payload;
	}
}

