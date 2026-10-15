package com.carpass.service_api.exception;

/**
 * Operacao existente, porem nao permitida para o usuario ou para o estado atual
 * do recurso. Resulta em HTTP 403.
 */
public class ForbiddenOperationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
