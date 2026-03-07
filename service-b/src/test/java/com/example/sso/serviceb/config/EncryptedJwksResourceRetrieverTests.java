package com.example.sso.serviceb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import org.junit.jupiter.api.Test;

class EncryptedJwksResourceRetrieverTests {

	@Test
	void decryptsPayloadAndReturnsPlainJwksJson() throws Exception {
		ResourceRetriever delegate = url -> new Resource("{\"format\":\"aes-gcm+base64\",\"payload\":\"enc-data\"}", "application/json");
		JwksPayloadCryptoService cryptoService = mock(JwksPayloadCryptoService.class);
		when(cryptoService.decryptFromBase64("enc-data")).thenReturn("{\"keys\":[{\"kty\":\"RSA\"}]}");

		EncryptedJwksResourceRetriever retriever = new EncryptedJwksResourceRetriever(delegate, cryptoService, new ObjectMapper());
		Resource result = retriever.retrieveResource(new URL("http://localhost:9000/.well-known/jwks.json"));

		assertThat(result.getContent()).contains("\"keys\"");
	}

	@Test
	void failsWhenEncryptedPayloadIsMissing() throws Exception {
		ResourceRetriever delegate = url -> new Resource("{\"format\":\"aes-gcm+base64\"}", "application/json");
		JwksPayloadCryptoService cryptoService = mock(JwksPayloadCryptoService.class);
		EncryptedJwksResourceRetriever retriever = new EncryptedJwksResourceRetriever(delegate, cryptoService, new ObjectMapper());

		assertThatThrownBy(() -> retriever.retrieveResource(new URL("http://localhost:9000/.well-known/jwks.json")))
				.isInstanceOf(Exception.class)
				.hasMessageContaining("Failed to decode encrypted JWKS response");
	}
}
