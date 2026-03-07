package com.example.sso.serviceb.api;

import java.util.Map;

import com.example.sso.serviceb.service.SecureBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/b")
@Tag(name = "Service B")
public class SecureBController {

	private final SecureBService secureBService;

	public SecureBController(SecureBService secureBService) {
		this.secureBService = secureBService;
	}

	@GetMapping("/secure")
	@Operation(summary = "Protected Service B endpoint (SAML session required)")
	public ResponseEntity<ApiResponse<Map<String, Object>>> secure(Authentication authentication) {
		Map<String, Object> payload = secureBService.securePayload(authentication);
		return ResponseEntity.ok(ApiResponse.ok("Access granted by service-b", payload));
	}
}
