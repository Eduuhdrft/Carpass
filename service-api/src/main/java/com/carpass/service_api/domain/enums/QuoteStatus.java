package com.carpass.service_api.domain.enums;

/** QuoteStatus. */
public enum QuoteStatus {

	NOT_REQUIRED("Nao requer aprovacao"),

	PENDING("Aguardando aprovacao"),

	APPROVED("Aprovado"),

	REJECTED("Recusado");

	private final String label;

	QuoteStatus(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
