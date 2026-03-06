package com.example.sso.servicea.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.sso.servicea.api.ApiResponse;
import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.service.AuthService;
import com.example.sso.servicea.service.RoleScopeService;

@RestControllerAdvice
public class CommonApiExceptionHandler {

	@ExceptionHandler(AuthService.UserAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(AuthService.UserAlreadyExistsException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error("USER_EXISTS", ex.getMessage()));
	}

	@ExceptionHandler(AuthService.InvalidRegistrationException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidRegistration(AuthService.InvalidRegistrationException ex) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.error("INVALID_REQUEST", ex.getMessage()));
	}

	@ExceptionHandler(AuthService.InvalidCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(AuthService.InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiResponse.error("INVALID_CREDENTIALS", ex.getMessage()));
	}

	@ExceptionHandler(RoleScopeService.InvalidRoleScopeException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidRoleScope(RoleScopeService.InvalidRoleScopeException ex) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.error("INVALID_ROLE_SCOPE", ex.getMessage()));
	}

	@ExceptionHandler(RoleScopeService.RoleNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleRoleNotFound(RoleScopeService.RoleNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("ROLE_NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(AppUserAccountService.UserNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleUserNotFound(AppUserAccountService.UserNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("USER_NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(AppUserAccountService.RoleNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleRoleNotFoundForUser(AppUserAccountService.RoleNotFoundException ex) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.error("ROLE_NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.error("INVALID_REQUEST", ex.getMessage()));
	}
}
