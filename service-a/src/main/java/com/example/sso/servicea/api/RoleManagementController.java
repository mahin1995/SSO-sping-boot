package com.example.sso.servicea.api;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.service.RoleScopeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Role Management")
public class RoleManagementController {

	private final RoleScopeService roleScopeService;
	private final AppUserAccountService appUserAccountService;

	public RoleManagementController(
			RoleScopeService roleScopeService,
			AppUserAccountService appUserAccountService
	) {
		this.roleScopeService = roleScopeService;
		this.appUserAccountService = appUserAccountService;
	}

	@PostMapping("/roles")
	@Operation(summary = "Create or update a role with scopes", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ApiResponse<RoleScopeService.RoleScopeView>> createOrUpdateRole(@RequestBody RoleRequest request) {
		RoleScopeService.RoleScopeView view = roleScopeService.createOrUpdateRole(request.roleName(), request.description(), request.scopes());
		return ResponseEntity.ok(ApiResponse.ok("Role updated", view));
	}

	@GetMapping("/roles/{roleName}")
	@Operation(summary = "Get role and its scopes", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ApiResponse<RoleScopeService.RoleScopeView>> getRole(@PathVariable String roleName) {
		RoleScopeService.RoleScopeView view = roleScopeService.getRole(roleName);
		return ResponseEntity.ok(ApiResponse.ok("Role fetched", view));
	}

	@PostMapping("/users/{username}/roles")
	@Operation(summary = "Assign roles to a user", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ApiResponse<AppUserAccountService.UserRoleScopeView>> assignRoles(
			@PathVariable String username,
			@RequestBody AssignRolesRequest request
	) {
		AppUserAccountService.UserRoleScopeView result = appUserAccountService.assignRoles(username, request.roles());
		return ResponseEntity.ok(ApiResponse.ok("User roles updated", result));
	}

	@GetMapping("/users/{username}/scopes")
	@Operation(summary = "Get effective scopes for a user from assigned roles", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ApiResponse<AppUserAccountService.UserRoleScopeView>> getUserScopes(@PathVariable String username) {
		AppUserAccountService.UserRoleScopeView result = appUserAccountService.getUserRoleScopeView(username);
		return ResponseEntity.ok(ApiResponse.ok("User effective scopes fetched", result));
	}

	public record RoleRequest(String roleName, String description, Set<String> scopes) {
	}

	public record AssignRolesRequest(Set<String> roles) {
	}
}
