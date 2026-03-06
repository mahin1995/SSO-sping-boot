package com.example.sso.servicea.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemUtils {

	private PemUtils() {
	}

	static RSAPrivateKey readPrivateKey(String pem) {
		try {
			String normalized = normalizePem(pem, "PRIVATE KEY");
			byte[] decoded = Base64.getMimeDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
			return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to read RSA private key", ex);
		}
	}

	static RSAPublicKey readPublicKey(String pem) {
		try {
			String normalized = normalizePem(pem, "PUBLIC KEY");
			byte[] decoded = Base64.getMimeDecoder().decode(normalized.getBytes(StandardCharsets.UTF_8));
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to read RSA public key", ex);
		}
	}

	private static String normalizePem(String pem, String marker) {
		return pem
				.replace("-----BEGIN " + marker + "-----", "")
				.replace("-----END " + marker + "-----", "")
				.replaceAll("\\s+", "");
	}
}

