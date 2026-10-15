package com.carpass.service_api.web.dto.customer;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.CustomerType;
import com.carpass.service_api.domain.model.Customer;
import com.carpass.service_api.web.dto.common.AddressResponse;

/**
 * Cliente devolvido pela API.
 */
public record CustomerResponse(UUID id, CustomerType type, String typeLabel, String name, String document, String phone,
		String email, AddressResponse address, String notes, boolean active, LocalDateTime createdAt) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getType(), customer.getType().getLabel(),
				customer.getName(), customer.getDocument(), customer.getPhone(), customer.getEmail(),
				AddressResponse.from(customer.getAddress()), customer.getNotes(), customer.isActive(),
				customer.getCreatedAt());
	}
}
