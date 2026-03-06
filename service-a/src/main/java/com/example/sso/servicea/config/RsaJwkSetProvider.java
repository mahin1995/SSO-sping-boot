package com.example.sso.servicea.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RsaJwkSetProvider {

	private static final Logger log = LoggerFactory.getLogger(RsaJwkSetProvider.class);

	private final Environment environment;

	public RsaJwkSetProvider(Environment environment) {
		this.environment = environment;
	}

	public JWKSet build() {
		RsaKeyPair rsaKeyPair = resolveActiveRsaKeyPair();
		String keyId = environment.getProperty("service-a.keys.key-id", "service-a-key");
		RSAKey activeKey = new RSAKey.Builder(rsaKeyPair.publicKey())
				.privateKey(rsaKeyPair.privateKey())
				.keyID(keyId)
				.build();

		List<RSAKey> keys = new ArrayList<>();
		keys.add(activeKey);
		keys.addAll(loadRetiredPublicKeys());
		return new JWKSet(new ArrayList<>(keys));
	}

	private RsaKeyPair resolveActiveRsaKeyPair() {
		Optional<RsaKeyPair> keyPairFromPemProperties = loadKeysFromRawPem();
		if (keyPairFromPemProperties.isPresent()) {
			return keyPairFromPemProperties.get();
		}

		Optional<RsaKeyPair> keyPairFromFiles = loadKeysFromFiles();
		if (keyPairFromFiles.isPresent()) {
			return keyPairFromFiles.get();
		}

		log.warn("No RSA keys configured for service-a. Generating ephemeral keys for this runtime.");
		return generateEphemeralKeyPair();
	}

	private Optional<RsaKeyPair> loadKeysFromRawPem() {
		String privatePem = environment.getProperty("service-a.keys.private-key-pem");
		String publicPem = environment.getProperty("service-a.keys.public-key-pem");
		if (isBlank(privatePem) || isBlank(publicPem)) {
			return Optional.empty();
		}
		return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
	}

	private Optional<RsaKeyPair> loadKeysFromFiles() {
		String privatePath = environment.getProperty("service-a.keys.private-key-path");
		String publicPath = environment.getProperty("service-a.keys.public-key-path");
		if (isBlank(privatePath) || isBlank(publicPath)) {
			return Optional.empty();
		}
		try {
			String privatePem = Files.readString(Path.of(privatePath));
			String publicPem = Files.readString(Path.of(publicPath));
			return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load RSA key files for service-a", ex);
		}
	}

	private List<RSAKey> loadRetiredPublicKeys() {
		String configuredRetiredKeys = environment.getProperty("service-a.keys.retired-public-keys");
		if (isBlank(configuredRetiredKeys)) {
			return List.of();
		}

		List<RSAKey> retiredKeys = new ArrayList<>();
		String[] entries = configuredRetiredKeys != null ? configuredRetiredKeys.split(";") : new String[0];
		for (String rawEntry : entries) {
			String entry = rawEntry == null ? "" : rawEntry.trim();
			if (entry.isEmpty()) {
				continue;
			}
			int separatorIndex = entry.indexOf('=');
			if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
				throw new IllegalStateException(
						"Invalid service-a.keys.retired-public-keys entry: " + entry + ". Expected format: <kid>=<path>"
				);
			}

			String retiredKeyId = entry.substring(0, separatorIndex).trim();
			String retiredPublicPath = entry.substring(separatorIndex + 1).trim();
			if (retiredKeyId.isEmpty() || retiredPublicPath.isEmpty()) {
				throw new IllegalStateException(
						"Invalid service-a.keys.retired-public-keys entry: " + entry + ". kid/path must be non-empty."
				);
			}

			try {
				String publicPem = Files.readString(Path.of(retiredPublicPath));
				RSAPublicKey publicKey = PemUtils.readPublicKey(publicPem);
				retiredKeys.add(new RSAKey.Builder(publicKey).keyID(retiredKeyId).build());
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Failed to load retired public key '" + retiredKeyId + "' from path: " + retiredPublicPath,
						ex
				);
			}
		}
		return retiredKeys;
	}

	private RsaKeyPair generateEphemeralKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			return new RsaKeyPair((RSAPrivateKey) keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to generate ephemeral RSA key pair", ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
	}
}
