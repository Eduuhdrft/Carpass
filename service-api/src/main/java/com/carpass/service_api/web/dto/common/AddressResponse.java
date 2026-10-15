package com.carpass.service_api.web.dto.common;

import com.carpass.service_api.domain.model.Address;

/**
 * Endereco devolvido pela API.
 */
public record AddressResponse(String zipCode, String street, String number, String complement, String neighborhood,
		String city, String state) {

	public static AddressResponse from(Address address) {
		if (address == null) {
			return null;
		}
		return new AddressResponse(address.getZipCode(), address.getStreet(), address.getNumber(),
				address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState());
	}
}
