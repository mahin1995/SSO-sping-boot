package com.example.sso.servicea.service;

import java.util.Set;

import com.example.sso.servicea.service.AppUserAccountService.UserRoleScopeView;
import com.example.sso.servicea.user.entity.AppUserEntity;

public interface AppUserAccountService {

	boolean existsByUsername(String username);

	AppUserEntity createUser(String username, String encodedPassword, Set<String> roleNames);

	void createUserIfMissing(String username, String encodedPassword, Set<String> roleNames);

	UserRoleScopeView getUserRoleScopeView(String username);

	UserRoleScopeView assignRoles(String username, Set<String> roleNames);

	record UserRoleScopeView(String username, Set<String> roles, Set<String> scopes) {
	}

	class UserNotFoundException extends RuntimeException {
		public UserNotFoundException(String message) {
			super(message);
		}
	}

	class RoleNotFoundException extends RuntimeException {
		public RoleNotFoundException(String message) {
			super(message);
		}
	}
}
