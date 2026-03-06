package com.example.sso.servicea.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JweTokenService {

	private final SecretKey jweKey;

	public JweTokenService(@Value("${service-a.jwe.secret:change-me-jwe-secret}") String rawSecret) {
		this.jweKey = deriveAesKey(rawSecret);
	}

	public String encryptSignedJwt(String signedJwt) {
		if (signedJwt == null || signedJwt.isBlank()) {
			throw new IllegalArgumentException("signedJwt is required");
		}

		try {
			JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
					.contentType("JWT")
					.build();
			JWEObject jweObject = new JWEObject(header, new Payload(signedJwt));
			jweObject.encrypt(new DirectEncrypter(jweKey));
			return jweObject.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Failed to encrypt signed JWT as JWE", ex);
		}
	}

	public String decryptToSignedJwt(String serializedJwe) {
		if (serializedJwe == null || serializedJwe.isBlank()) {
			throw new IllegalArgumentException("serializedJwe is required");
		}

		try {
			JWEObject jweObject = JWEObject.parse(serializedJwe);
			jweObject.decrypt(new DirectDecrypter(jweKey));
			String signedJwt = jweObject.getPayload().toString();
			if (signedJwt == null || signedJwt.isBlank()) {
				throw new IllegalStateException("JWE payload is empty");
			}
			return signedJwt;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to decrypt JWE token", ex);
		}
	}

	private SecretKey deriveAesKey(String rawSecret) {
		if (rawSecret == null || rawSecret.isBlank()) {
			throw new IllegalStateException("service-a.jwe.secret is required");
		}
		try {
			byte[] hashed = MessageDigest.getInstance("SHA-256")
					.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
			String encoded = Base64.getEncoder().encodeToString(hashed);
			byte[] keyBytes = Base64.getDecoder().decode(encoded);
			return new SecretKeySpec(keyBytes, "AES");
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to derive JWE key", ex);
		}
	}
}
