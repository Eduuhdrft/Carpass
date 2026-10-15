package com.carpass.service_api.web.dto.workshop;

import com.carpass.service_api.web.dto.common.AddressRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Os dados de uma oficina nova.
 *
 * <p>
 * Serve aos dois caminhos que criam oficina: o cadastro publico, onde a pessoa
 * ainda nao existe, e a criacao por quem ja esta autenticado -- tipicamente
 * alguem que saiu de uma oficina e vai abrir a propria. Um DTO so para os dois
 * evita que as validacoes do CNPJ e do endereco divirjam entre os caminhos.
 * </p>
 */
public record CreateWorkshopRequest(

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
		AddressRequest address) {
}
