package com.example.sso.serviceb.config;

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

	public StartupEnvDiagnostics(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logStartupEnvSnapshot() {
		log.info(
				"service-b env snapshot: tokenMode='{}', issuer='{}', jwkSetUri='{}', audience='{}'",
				environment.getProperty("service-b.token.mode", "<missing>"),
				environment.getProperty("service-b.jwt.issuer", "<missing>"),
				environment.getProperty("service-b.jwt.jwk-set-uri", "<missing>"),
				environment.getProperty("service-b.audience", "<missing>")
		);

		logSecretState("service-b.jwe.secret");
		logSecretState("service-b.jwks.decrypt-secret");
		logSecretState("service-b.introspection.secret");
	}

	private void logSecretState(String propertyName) {
		String value = environment.getProperty(propertyName);
		boolean present = value != null && !value.isBlank();
		int length = present ? value.length() : 0;
		log.info("{} present={} length={}", propertyName, present, length);
	}
}
