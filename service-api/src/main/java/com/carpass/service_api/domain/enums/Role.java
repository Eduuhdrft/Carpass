package com.carpass.service_api.domain.enums;

/** Role. */
public enum Role {

	SYSTEM_ADMIN("Administrador do sistema"),

	OWNER("Proprietario"),

	MANAGER("Gerente"),

	RECEPTIONIST("Atendente"),

	MECHANIC("Mecanico");

	private final String label;

	Role(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado em relatorios e mensagens. */
	public String getLabel() {
		return label;
	}
}
