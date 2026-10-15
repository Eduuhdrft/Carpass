package com.carpass.service_api.domain.enums;

/** ItemType. */
public enum ItemType {

	SERVICE("Servico"),

	PART("Peca");

	private final String label;

	ItemType(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
