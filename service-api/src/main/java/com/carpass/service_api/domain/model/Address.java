package com.carpass.service_api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Endereco reaproveitado por oficinas e clientes.
 */
@Embeddable
public class Address {

	@Column(name = "zip_code", length = 10)
	private String zipCode;

	@Column(name = "street", length = 180)
	private String street;

	@Column(name = "street_number", length = 20)
	private String number;

	@Column(name = "complement", length = 120)
	private String complement;

	@Column(name = "neighborhood", length = 120)
	private String neighborhood;

	@Column(name = "city", length = 120)
	private String city;

	@Column(name = "state", length = 2)
	private String state;

	public Address() {
		// construtor sem argumentos exigido pelo JPA
	}

	public Address(String zipCode, String street, String number, String complement, String neighborhood, String city,
			String state) {
		this.zipCode = zipCode;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.neighborhood = neighborhood;
		this.city = city;
		this.state = state;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getComplement() {
		return complement;
	}

	public void setComplement(String complement) {
		this.complement = complement;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public void setNeighborhood(String neighborhood) {
		this.neighborhood = neighborhood;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
