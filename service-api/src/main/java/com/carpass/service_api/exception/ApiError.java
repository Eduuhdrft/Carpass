package com.carpass.service_api.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corpo padrao de erro da API. Nunca carrega stack trace nem detalhe interno.
 *
 * @param timestamp momento em que o erro ocorreu
 * @param status    codigo HTTP
 * @param error     nome curto do status (ex.: "Not Found")
 * @param message   mensagem amigavel para o usuario final
 * @param path      caminho da requisicao
 * @param details   erros de validacao campo a campo, quando houver
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldErrorDetail> details) {

	/** Erro de validacao de um campo especifico. */
	public record FieldErrorDetail(String field, String message) {
	}
}
