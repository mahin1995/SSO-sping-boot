package com.example.sso.serviceb.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AlgorithmValidator implements OAuth2TokenValidator<Jwt> {

	private final String requiredAlgorithm;
	private final OAuth2Error error = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"The token signing algorithm is not allowed",
			null
	);

	public AlgorithmValidator(String requiredAlgorithm) {
		this.requiredAlgorithm = requiredAlgorithm;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Object algorithmHeader = token.getHeaders().get("alg");
		String algorithm = algorithmHeader == null ? "" : algorithmHeader.toString();
		if (requiredAlgorithm.equals(algorithm)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(error);
	}
}
