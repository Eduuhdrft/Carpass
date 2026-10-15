package com.carpass.service_api.web.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cria a conta de acesso de um funcionario.
 *
 * <p>
 * Nao recebemos senha aqui de proposito: a conta nasce sem senha utilizavel e o
 * funcionario define a dele pelo link de primeiro acesso enviado por e-mail.
 * Assim ninguem, nem o gerente, chega a conhecer a senha de outra pessoa.
 * </p>
 */
public record GrantAccessRequest(

		@NotBlank(message = "Informe o e-mail de acesso")
		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email) {
}
