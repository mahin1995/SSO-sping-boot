package com.example.sso.servicea.token.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "opaque_access_tokens")
public class OpaqueAccessTokenEntity {

	@Id
	@Column(name = "token_hash", length = 64, nullable = false)
	private String tokenHash;

	@Column(name = "subject", length = 200, nullable = false)
	private String subject;

	@Column(name = "scope", length = 2000, nullable = false)
	private String scope;

	@Column(name = "audience", length = 1000, nullable = false)
	private String audience;

	@Column(name = "issuer", length = 200, nullable = false)
	private String issuer;

	@Column(name = "token_id", length = 100, nullable = false)
	private String tokenId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "active", nullable = false)
	private boolean active;

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getAudience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = audience;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(Instant issuedAt) {
		this.issuedAt = issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
