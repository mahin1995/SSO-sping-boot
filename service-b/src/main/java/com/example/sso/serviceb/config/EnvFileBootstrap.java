package com.example.sso.serviceb.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EnvFileBootstrap {

	private EnvFileBootstrap() {
	}

	public static void loadForServiceB() {
		List<Path> candidates = List.of(
				Path.of(".env.service-b"),
				Path.of("service-b/.env.service-b"),
				Path.of("../service-b/.env.service-b"),
				Path.of(".env")
		);
		loadFirstExisting(candidates, "service-b");
	}

	private static void loadFirstExisting(List<Path> candidates, String serviceName) {
		for (Path candidate : candidates) {
			if (!Files.exists(candidate)) {
				continue;
			}

			loadFromFile(candidate, serviceName);
			return;
		}

		System.out.println("[env-bootstrap][" + serviceName + "] No .env file found in known locations.");
	}

	private static void loadFromFile(Path file, String serviceName) {
		Path resolved = file.toAbsolutePath().normalize();
		int loadedCount = 0;

		try {
			for (String rawLine : Files.readAllLines(resolved, StandardCharsets.UTF_8)) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int separator = line.indexOf('=');
				if (separator <= 0) {
					continue;
				}

				String key = line.substring(0, separator).trim();
				String value = line.substring(separator + 1).trim();
				if (key.isEmpty()) {
					continue;
				}

				if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}

				if (System.getProperty(key) == null && System.getenv(key) == null) {
					System.setProperty(key, value);
					loadedCount++;
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load env file: " + resolved, ex);
		}

		System.out.println(
				"[env-bootstrap][" + serviceName + "] Loaded " + loadedCount + " vars from " + resolved
		);
	}
}
