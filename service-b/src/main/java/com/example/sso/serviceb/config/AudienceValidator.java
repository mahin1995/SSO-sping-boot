package com.example.sso.serviceb.config;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

	private final String requiredAudience;
	private final OAuth2Error error = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"The token audience is not valid",
			null
	);

	public AudienceValidator(String requiredAudience) {
		this.requiredAudience = requiredAudience;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		List<String> audience = token.getAudience();
		if (audience != null && audience.contains(requiredAudience)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(error);
	}
}

