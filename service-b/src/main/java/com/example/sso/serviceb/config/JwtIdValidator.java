package com.example.sso.serviceb.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtIdValidator implements OAuth2TokenValidator<Jwt> {

	private final OAuth2Error error = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"The token jti claim is missing",
			null
	);

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		String tokenId = token.getId();
		if (tokenId != null && !tokenId.isBlank()) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(error);
	}
}
