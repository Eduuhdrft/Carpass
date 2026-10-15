package com.carpass.service_api.web.dto.common;

import com.carpass.service_api.domain.model.Address;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Endereco recebido do cliente.
 */
public record AddressRequest(

		@Pattern(regexp = "^$|\\d{8}", message = "O CEP deve conter 8 digitos, sem pontuacao")
		String zipCode,

		@Size(max = 180) String street,

		@Size(max = 20) String number,

		@Size(max = 120) String complement,

		@Size(max = 120) String neighborhood,

		@Size(max = 120) String city,

		@Pattern(regexp = "^$|[A-Za-z]{2}", message = "Use a sigla do estado com 2 letras")
		String state) {

	/** Converte para o objeto embutido da entidade. */
	public Address toAddress() {
		return new Address(zipCode, street, number, complement, neighborhood, city,
				state == null ? null : state.toUpperCase());
	}
}
