package com.example.sso.servicea.api;

import java.util.Map;

import com.example.sso.servicea.config.JwksPayloadCryptoService;
import com.example.sso.servicea.config.RsaJwkSetProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

	private final RsaJwkSetProvider rsaJwkSetProvider;
	private final JwksPayloadCryptoService jwksPayloadCryptoService;

	public JwksController(RsaJwkSetProvider rsaJwkSetProvider, JwksPayloadCryptoService jwksPayloadCryptoService) {
		this.rsaJwkSetProvider = rsaJwkSetProvider;
		this.jwksPayloadCryptoService = jwksPayloadCryptoService;
	}

	@GetMapping({"/.well-known/jwks.json", "/api/auth/jwks"})
	public Map<String, Object> jwks() {
		String jwksJson = rsaJwkSetProvider.build().toPublicJWKSet().toString();
		return Map.of(
				"format", "aes-gcm+base64",
				"payload", jwksPayloadCryptoService.encryptToBase64(jwksJson)
		);
	}
}
