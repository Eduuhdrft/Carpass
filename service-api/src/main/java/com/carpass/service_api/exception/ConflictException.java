package com.carpass.service_api.exception;

/**
 * Conflito com o estado atual do recurso, como duplicidade. Resulta em HTTP 409.
 */
public class ConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConflictException(String message) {
		super(message);
	}
}
