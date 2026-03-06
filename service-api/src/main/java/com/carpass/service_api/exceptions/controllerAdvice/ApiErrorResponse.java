package com.carpass.service_api.exceptions.controllerAdvice;

public record ApiErrorResponse(
		Integer status,
		String error,
		String message
	) {}
