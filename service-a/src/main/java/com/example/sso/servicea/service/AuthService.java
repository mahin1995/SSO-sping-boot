package com.example.sso.servicea.service;

import java.util.List;
import java.util.Set;

public interface AuthService {

	RegisteredUser register(RegisterUserCommand command);

	LoginToken login(LoginCommand command);

	LoginToken issueServiceToken(ServiceTokenCommand command);

	record RegisterUserCommand(String username, String password, Set<String> roles) {
	}

	record RegisteredUser(String username, Set<String> roles, Set<String> scopes) {
	}

	record LoginCommand(String username, String password) {
	}

	record ServiceTokenCommand(String clientId, String clientSecret) {
	}

	record LoginToken(
			String accessToken,
			String tokenType,
			long expiresIn,
			String scope,
			List<String> audience
	) {
	}

	class UserAlreadyExistsException extends RuntimeException {
		public UserAlreadyExistsException(String message) {
			super(message);
		}
	}

	class InvalidRegistrationException extends RuntimeException {
		public InvalidRegistrationException(String message) {
			super(message);
		}
	}

	class InvalidCredentialsException extends RuntimeException {
		public InvalidCredentialsException(String message) {
			super(message);
		}
	}
}
