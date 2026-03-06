package com.carpass.service_api.exceptions;

public class VeiculoNaoEncontradoException extends RuntimeException {
	public VeiculoNaoEncontradoException() {
		super("Veículo não encontrado");
	}
}
