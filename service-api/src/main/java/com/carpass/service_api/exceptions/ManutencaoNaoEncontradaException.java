package com.carpass.service_api.exceptions;

public class ManutencaoNaoEncontradaException extends RuntimeException {
	public ManutencaoNaoEncontradaException() {
		super("Manutenção não encontrada");
	}
}
