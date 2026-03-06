package com.example.sso.servicea.api;

import java.time.Instant;
import java.util.List;

import com.example.sso.servicea.config.TokenMode;
import com.example.sso.servicea.service.OpaqueTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class OpaqueTokenIntrospectionController {

	private final OpaqueTokenService opaqueTokenService;
	private final TokenMode tokenMode;
	private final String introspectionSecret;

	public OpaqueTokenIntrospectionController(
			OpaqueTokenService opaqueTokenService,
			@Value("${service-a.token.mode:jwe}") String tokenModeRaw,
			@Value("${service-a.introspection.secret:change-me-introspection-secret}") String introspectionSecret
	) {
		this.opaqueTokenService = opaqueTokenService;
		this.tokenMode = TokenMode.from(tokenModeRaw);
		this.introspectionSecret = introspectionSecret;
	}

	@PostMapping("/introspect")
	@Operation(summary = "Introspect opaque access token (used by service-b)", security = {})
	public ResponseEntity<ApiResponse<IntrospectionResponse>> introspect(
			@RequestBody IntrospectionRequest request,
			@RequestHeader(name = "X-Introspection-Secret", required = false) String providedSecret
	) {
		if (tokenMode != TokenMode.OPAQUE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Opaque token mode is disabled");
		}
		if (providedSecret == null || !providedSecret.equals(introspectionSecret)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid introspection secret");
		}

		OpaqueTokenService.OpaqueTokenIntrospection introspection = opaqueTokenService.introspect(request.token());
		IntrospectionResponse response = new IntrospectionResponse(
				introspection.active(),
				introspection.subject(),
				introspection.scopeAsString(),
				introspection.audience(),
				toEpochSecond(introspection.expiresAt()),
				toEpochSecond(introspection.issuedAt()),
				introspection.issuer(),
				introspection.tokenId()
		);
		return ResponseEntity.ok(ApiResponse.ok("Token introspection result", response));
	}

	private Long toEpochSecond(Instant instant) {
		return instant == null ? null : instant.getEpochSecond();
	}

	public record IntrospectionRequest(String token) {
	}

	public record IntrospectionResponse(
			boolean active,
			String sub,
			String scope,
			List<String> audience,
			Long exp,
			Long iat,
			String iss,
			String jti
	) {
	}
}
