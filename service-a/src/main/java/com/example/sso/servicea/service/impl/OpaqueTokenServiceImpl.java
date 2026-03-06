package com.example.sso.servicea.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.sso.servicea.service.OpaqueTokenService;
import com.example.sso.servicea.token.entity.OpaqueAccessTokenEntity;
import com.example.sso.servicea.token.repository.OpaqueAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpaqueTokenServiceImpl implements OpaqueTokenService {

	private final OpaqueAccessTokenRepository opaqueAccessTokenRepository;

	public OpaqueTokenServiceImpl(OpaqueAccessTokenRepository opaqueAccessTokenRepository) {
		this.opaqueAccessTokenRepository = opaqueAccessTokenRepository;
	}

	@Override
	@Transactional
	public String issueToken(
			String subject,
			Set<String> scopes,
			List<String> audience,
			Instant issuedAt,
			Instant expiresAt,
			String issuer,
			String tokenId
	) {
		String tokenValue = generateOpaqueTokenValue();

		OpaqueAccessTokenEntity entity = new OpaqueAccessTokenEntity();
		entity.setTokenHash(hashToken(tokenValue));
		entity.setSubject(subject);
		entity.setScope(String.join(" ", scopes));
		entity.setAudience(String.join(",", audience));
		entity.setIssuer(issuer);
		entity.setTokenId(tokenId);
		entity.setIssuedAt(issuedAt);
		entity.setExpiresAt(expiresAt);
		entity.setActive(true);
		opaqueAccessTokenRepository.save(entity);

		return tokenValue;
	}

	@Override
	@Transactional(readOnly = true)
	public OpaqueTokenIntrospection introspect(String tokenValue) {
		if (tokenValue == null || tokenValue.isBlank()) {
			return OpaqueTokenIntrospection.inactive();
		}

		return opaqueAccessTokenRepository.findByTokenHashAndActiveTrue(hashToken(tokenValue))
				.map(entity -> toIntrospection(entity, Instant.now()))
				.orElse(OpaqueTokenIntrospection.inactive());
	}

	private OpaqueTokenIntrospection toIntrospection(OpaqueAccessTokenEntity entity, Instant now) {
		if (entity.getExpiresAt() == null || now.isAfter(entity.getExpiresAt())) {
			return OpaqueTokenIntrospection.inactive();
		}

		Set<String> scopes = new LinkedHashSet<>();
		if (entity.getScope() != null) {
			for (String scope : entity.getScope().split("\\s+")) {
				String normalized = scope.trim();
				if (!normalized.isEmpty()) {
					scopes.add(normalized);
				}
			}
		}

		List<String> audience = new ArrayList<>();
		if (entity.getAudience() != null) {
			for (String aud : entity.getAudience().split(",")) {
				String normalized = aud.trim();
				if (!normalized.isEmpty()) {
					audience.add(normalized);
				}
			}
		}

		return new OpaqueTokenIntrospection(
				true,
				entity.getSubject(),
				scopes,
				audience,
				entity.getIssuedAt(),
				entity.getExpiresAt(),
				entity.getIssuer(),
				entity.getTokenId()
		);
	}

	private String generateOpaqueTokenValue() {
		byte[] bytes = new byte[32];
		new java.security.SecureRandom().nextBytes(bytes);
		return "opq_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String tokenValue) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(digest.length * 2);
			for (byte value : digest) {
				builder.append(String.format("%02x", value & 0xff));
			}
			return builder.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to hash opaque token", ex);
		}
	}
}
