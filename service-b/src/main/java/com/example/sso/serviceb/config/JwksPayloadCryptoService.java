package com.example.sso.serviceb.config;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwksPayloadCryptoService {

	private static final int GCM_IV_BYTES = 12;
	private static final int GCM_TAG_BITS = 128;
	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

	private final SecretKey decryptionKey;

	public JwksPayloadCryptoService(@Value("${service-b.jwks.decrypt-secret}") String rawSecret) {
		this.decryptionKey = deriveAesKey(rawSecret);
	}

	public String decryptFromBase64(String encodedPayload) {
		if (encodedPayload == null || encodedPayload.isBlank()) {
			throw new IllegalArgumentException("encodedPayload is required");
		}

		try {
			byte[] combined = Base64.getDecoder().decode(encodedPayload);
			if (combined.length <= GCM_IV_BYTES) {
				throw new IllegalStateException("Encrypted JWKS payload is malformed");
			}

			ByteBuffer buffer = ByteBuffer.wrap(combined);
			byte[] iv = new byte[GCM_IV_BYTES];
			buffer.get(iv);
			byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] plaintext = cipher.doFinal(ciphertext);
			return new String(plaintext, StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to decrypt JWKS payload", ex);
		}
	}

	private SecretKey deriveAesKey(String rawSecret) {
		if (rawSecret == null || rawSecret.isBlank()) {
			throw new IllegalStateException("service-b.jwks.decrypt-secret is required");
		}
		try {
			byte[] keyBytes = MessageDigest.getInstance("SHA-256")
					.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(keyBytes, "AES");
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to derive JWKS decryption key", ex);
		}
	}
}
