package com.carpass.service_api.web.dto.auth;

import com.carpass.service_api.web.dto.common.AddressRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro inicial: cria a oficina e o usuario proprietario em uma unica
 * operacao. E o unico caminho publico para criar contas; qualquer outro usuario
 * e criado por alguem ja autenticado dentro da oficina.
 */
public record RegisterWorkshopRequest(

		@NotBlank(message = "Informe o nome fantasia da oficina")
		@Size(max = 150, message = "Nome fantasia muito longo")
		String tradeName,

		@Size(max = 150, message = "Razao social muito longa")
		String corporateName,

		@NotBlank(message = "Informe o CNPJ")
		@Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter 14 digitos, sem pontuacao")
		String cnpj,

		@Size(max = 30, message = "Telefone muito longo")
		String workshopPhone,

		@Email(message = "E-mail da oficina invalido")
		@Size(max = 180)
		String workshopEmail,

		@Valid
		AddressRequest address,

		@NotBlank(message = "Informe seu nome")
		@Size(max = 80)
		String firstName,

		@Size(max = 80)
		String lastName,

		@NotBlank(message = "Informe seu e-mail")
		@Email(message = "E-mail invalido")
		@Size(max = 180)
		String email,

		@Size(max = 30)
		String phone,

		@NotBlank(message = "Informe a senha")
		@Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
		@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve conter ao menos uma letra e um numero")
		String password) {
}
