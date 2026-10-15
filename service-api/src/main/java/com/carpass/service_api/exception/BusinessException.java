package com.carpass.service_api.exception;

/**
 * Violacao de regra de negocio. Resulta em HTTP 400.
 */
public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BusinessException(String message) {
		super(message);
	}
}
