package com.example.sso.servicea.config;

import java.util.Locale;

public enum TokenMode {
	JWE,
	OPAQUE;

	public static TokenMode from(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			throw new IllegalStateException("service-a.token.mode is required. Supported values: jwe, opaque");
		}

		String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
		try {
			return TokenMode.valueOf(normalized);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"Unsupported service-a.token.mode '" + rawValue + "'. Supported values: jwe, opaque",
					ex
			);
		}
	}
}
