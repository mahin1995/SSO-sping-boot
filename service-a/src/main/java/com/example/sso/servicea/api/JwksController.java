package com.example.sso.servicea.api;

import java.util.Map;

import com.example.sso.servicea.config.RsaJwkSetProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

	private final RsaJwkSetProvider rsaJwkSetProvider;

	public JwksController(RsaJwkSetProvider rsaJwkSetProvider) {
		this.rsaJwkSetProvider = rsaJwkSetProvider;
	}

	@GetMapping({"/.well-known/jwks.json", "/api/auth/jwks"})
	public Map<String, Object> jwks() {
		return rsaJwkSetProvider.build().toPublicJWKSet().toJSONObject();
	}
}
