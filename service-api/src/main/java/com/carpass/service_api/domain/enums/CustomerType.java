package com.carpass.service_api.domain.enums;

/** CustomerType. */
public enum CustomerType {

	INDIVIDUAL("Pessoa fisica"),

	COMPANY("Pessoa juridica");

	private final String label;

	CustomerType(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
