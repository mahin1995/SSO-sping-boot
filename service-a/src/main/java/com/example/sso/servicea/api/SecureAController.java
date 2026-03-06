package com.example.sso.servicea.api;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sso.servicea.service.SecureAService;

@RestController
@RequestMapping("/api/a")
@Tag(name = "Service A")
public class SecureAController {

	private final SecureAService secureAService;

	public SecureAController(SecureAService secureAService) {
		this.secureAService = secureAService;
	}

	@GetMapping("/secure")
	@Operation(summary = "Protected Service A endpoint", security = @SecurityRequirement(name = "bearerAuth"))
	public ResponseEntity<ApiResponse<Map<String, Object>>> secure(Authentication authentication) {
		Map<String, Object> payload = secureAService.securePayload(authentication);
		return ResponseEntity.ok(ApiResponse.ok("Access granted by service-a", payload));
	}
}
