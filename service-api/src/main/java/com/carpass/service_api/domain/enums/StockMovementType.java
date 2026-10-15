package com.carpass.service_api.domain.enums;

/** StockMovementType. */
public enum StockMovementType {

	INBOUND("Entrada"),

	OUTBOUND("Saida"),

	ADJUSTMENT("Ajuste");

	private final String label;

	StockMovementType(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
