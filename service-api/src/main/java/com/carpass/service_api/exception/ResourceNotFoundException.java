package com.carpass.service_api.exception;

/**
 * Recurso inexistente ou fora do escopo da oficina do usuario. Resulta em HTTP 404.
 *
 * <p>
 * Importante: quando um recurso existe mas pertence a outra oficina, tambem
 * lancamos 404 (e nao 403) para nao revelar a existencia do registro.
 * </p>
 */
public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}

	/** Atalho para a mensagem padrao "&lt;recurso&gt; nao encontrado(a)". */
	public static ResourceNotFoundException of(String resource) {
		return new ResourceNotFoundException(resource + " nao encontrado(a).");
	}
}
