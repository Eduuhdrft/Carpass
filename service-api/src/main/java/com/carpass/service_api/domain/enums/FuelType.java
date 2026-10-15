package com.carpass.service_api.domain.enums;

/** FuelType. */
public enum FuelType {

	GASOLINE("Gasolina"),

	ETHANOL("Etanol"),

	FLEX("Flex"),

	DIESEL("Diesel"),

	GNV("GNV"),

	ELECTRIC("Eletrico"),

	HYBRID("Hibrido");

	private final String label;

	FuelType(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
