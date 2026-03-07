package com.example.sso.servicea.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupEnvDiagnostics {

	private static final Logger log = LoggerFactory.getLogger(StartupEnvDiagnostics.class);

	private final Environment environment;
	private final Path projectRoot;

	public StartupEnvDiagnostics(Environment environment) {
		this.environment = environment;
		this.projectRoot = Path.of("").toAbsolutePath().normalize();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logStartupEnvSnapshot() {
		log.info(
				"service-a env snapshot: issuer='{}', tokenMode='{}', keyId='{}'",
				environment.getProperty("service-a.issuer", "<missing>"),
				environment.getProperty("service-a.token.mode", "<missing>"),
				environment.getProperty("service-a.keys.key-id", "<missing>")
		);
		logPathState("service-a.keys.private-key-path");
		logPathState("service-a.keys.public-key-path");
	}

	private void logPathState(String propertyName) {
		String rawValue = environment.getProperty(propertyName);
		if (rawValue == null || rawValue.isBlank()) {
			log.warn("{} is not set or empty.", propertyName);
			return;
		}

		try {
			Path configuredPath = Path.of(rawValue.trim());
			Path resolvedPath = configuredPath.isAbsolute()
					? configuredPath.normalize()
					: projectRoot.resolve(configuredPath).normalize();

			log.info(
					"{}='{}' resolved='{}' exists={} insideProject={}",
					propertyName,
					rawValue,
					resolvedPath,
					Files.exists(resolvedPath),
					resolvedPath.startsWith(projectRoot)
			);
		}
		catch (Exception ex) {
			log.warn("Invalid path value for {}='{}'.", propertyName, rawValue);
		}
	}
}
