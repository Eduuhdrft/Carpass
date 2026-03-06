package com.carpass.service_api.exceptions;

public class UsuarioNaoEncontradoException extends RuntimeException{
	public UsuarioNaoEncontradoException() {
		super("Usuário não encontrado");
	}
}
