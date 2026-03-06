package com.example.sso.servicea.service;

import java.util.Set;

public interface RoleScopeService {

	RoleScopeView createOrUpdateRole(String roleName, String description, Set<String> scopes);

	RoleScopeView getRole(String roleName);

	record RoleScopeView(String roleName, String description, Set<String> scopes) {
	}

	class InvalidRoleScopeException extends RuntimeException {
		public InvalidRoleScopeException(String message) {
			super(message);
		}
	}

	class RoleNotFoundException extends RuntimeException {
		public RoleNotFoundException(String message) {
			super(message);
		}
	}
}
