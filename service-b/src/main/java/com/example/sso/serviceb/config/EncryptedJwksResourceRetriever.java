package com.example.sso.serviceb.config;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;

public class EncryptedJwksResourceRetriever implements ResourceRetriever {

	private final ResourceRetriever delegate;
	private final JwksPayloadCryptoService jwksPayloadCryptoService;
	private final ObjectMapper objectMapper;

	public EncryptedJwksResourceRetriever(
			ResourceRetriever delegate,
			JwksPayloadCryptoService jwksPayloadCryptoService,
			ObjectMapper objectMapper
	) {
		this.delegate = delegate;
		this.jwksPayloadCryptoService = jwksPayloadCryptoService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Resource retrieveResource(URL url) throws IOException {
		Resource response = delegate.retrieveResource(url);
		String body = response.getContent();
		if (body == null || body.isBlank()) {
			throw new IOException("Encrypted JWKS response is empty");
		}

		try {
			JsonNode root = objectMapper.readTree(body);
			JsonNode payloadNode = root.path("payload");
			if (payloadNode.isMissingNode() || payloadNode.asText().isBlank()) {
				throw new IOException("Encrypted JWKS response does not contain 'payload'");
			}

			String decryptedJwks = jwksPayloadCryptoService.decryptFromBase64(payloadNode.asText());
			return new Resource(decryptedJwks, response.getContentType());
		}
		catch (Exception ex) {
			throw new IOException("Failed to decode encrypted JWKS response", ex);
		}
	}
}
