package com.example.sso.servicea.api;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sso.servicea.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@CrossOrigin("*")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@Operation(summary = "Create a new user with an encoded password", security = {})
	public ResponseEntity<ApiResponse<AuthService.RegisteredUser>> register(@RequestBody RegisterUserRequest request) {
		AuthService.RegisteredUser registeredUser = authService.register(
				new AuthService.RegisterUserCommand(request.username(), request.password(), request.roles())
		);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("User created", registeredUser));
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate with username/password and return JWT", security = {})
	public ResponseEntity<ApiResponse<AuthService.LoginToken>> login(@RequestBody LoginRequest request) {
		AuthService.LoginToken loginToken = authService.login(
				new AuthService.LoginCommand(request.username(), request.password())
		);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", loginToken));
	}

	public record RegisterUserRequest(String username, String password, Set<String> roles) {
	}

	public record LoginRequest(String username, String password) {
	}
}
