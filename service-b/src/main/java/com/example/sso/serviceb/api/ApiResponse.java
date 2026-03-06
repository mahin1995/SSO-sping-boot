package com.example.sso.serviceb.api;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String code,
		String message,
		T data,
		Instant timestamp
) {

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, "OK", message, data, Instant.now());
	}
}

