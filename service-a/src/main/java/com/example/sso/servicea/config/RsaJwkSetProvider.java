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
	private final Path projectRoot;

	public RsaJwkSetProvider(Environment environment) {
		this.environment = environment;
		this.projectRoot = Path.of("").toAbsolutePath().normalize();
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
			log.info("Loaded RSA keys for service-a from raw PEM properties.");
			return keyPairFromPemProperties.get();
		}

		Optional<RsaKeyPair> keyPairFromFiles = loadKeysFromFiles();
		if (keyPairFromFiles.isPresent()) {
			log.info("Loaded RSA keys for service-a from configured key files.");
			return keyPairFromFiles.get();
		}

		// Optional<RsaKeyPair> keyPairFromDefaultDevPaths = loadKeysFromDefaultDevPaths();
		// if (keyPairFromDefaultDevPaths.isPresent()) {
		// 	return keyPairFromDefaultDevPaths.get();
		// }

		log.warn(
				"No RSA keys configured for service-a. Generating ephemeral keys for this runtime. " +
						"Set SERVICE_A_PRIVATE_KEY_PATH and SERVICE_A_PUBLIC_KEY_PATH (or service-a.keys.* properties)."
		);
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

		Optional<Path> privateKeyPath = resolveProjectPath(privatePath, "service-a.keys.private-key-path");
		Optional<Path> publicKeyPath = resolveProjectPath(publicPath, "service-a.keys.public-key-path");
		if (privateKeyPath.isEmpty() || publicKeyPath.isEmpty()) {
			return Optional.empty();
		}

		try {
			String privatePem = Files.readString(privateKeyPath.get());
			String publicPem = Files.readString(publicKeyPath.get());
			return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load RSA key files for service-a", ex);
		}
	}

	private Optional<RsaKeyPair> loadKeysFromDefaultDevPaths() {
		List<KeyPathPair> candidates = List.of(
				new KeyPathPair(Path.of("keys/private.pem"), Path.of("keys/public.pem")),
				new KeyPathPair(Path.of("service-a/keys/private.pem"), Path.of("service-a/keys/public.pem"))
		);

		for (KeyPathPair candidate : candidates) {
			if (!Files.exists(candidate.privateKeyPath()) || !Files.exists(candidate.publicKeyPath())) {
				continue;
			}
			try {
				String privatePem = Files.readString(candidate.privateKeyPath());
				String publicPem = Files.readString(candidate.publicKeyPath());
				log.info(
						"Loaded RSA keys for service-a from default dev paths: private='{}', public='{}'.",
						candidate.privateKeyPath(),
						candidate.publicKeyPath()
				);
				return Optional.of(new RsaKeyPair(PemUtils.readPrivateKey(privatePem), PemUtils.readPublicKey(publicPem)));
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Failed to load RSA keys from default dev paths: " + candidate.privateKeyPath() + ", "
								+ candidate.publicKeyPath(),
						ex
				);
			}
		}
		return Optional.empty();
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
			Optional<Path> resolvedRetiredPath = resolveProjectPath(
					retiredPublicPath,
					"service-a.keys.retired-public-keys"
			);
			if (resolvedRetiredPath.isEmpty()) {
				continue;
			}

			try {
				String publicPem = Files.readString(resolvedRetiredPath.get());
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

	private Optional<Path> resolveProjectPath(String rawPath, String propertyName) {
		if (isBlank(rawPath)) {
			return Optional.empty();
		}

		try {
			Path inputPath = Path.of(rawPath.trim());
			Path resolvedPath = inputPath.isAbsolute()
					? inputPath.normalize()
					: projectRoot.resolve(inputPath).normalize();

			if (!resolvedPath.startsWith(projectRoot)) {
				log.warn(
						"Ignoring {} because path is outside project root. projectRoot='{}', configured='{}'",
						propertyName,
						projectRoot,
						rawPath
				);
				return Optional.empty();
			}
			return Optional.of(resolvedPath);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Invalid path for " + propertyName + ": " + rawPath, ex);
		}
	}

	private record KeyPathPair(Path privateKeyPath, Path publicKeyPath) {
	}

	private record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
	}
}
