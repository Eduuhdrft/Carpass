package com.carpass.service_api.domain.enums;

/**
 * Tipo de veiculo, na divisao que a tabela FIPE usa.
 *
 * <p>
 * A FIPE e tres tabelas separadas -- carro, moto e caminhao nao dividem a mesma
 * lista de marcas. Este enum e a ponte entre o vocabulario do sistema e o
 * caminho da API, e por isso carrega o segmento da URL: se um dia trocarmos de
 * provedor, o mapeamento muda em um lugar so.
 * </p>
 */
public enum VehicleKind {

	CAR("carros", "Carro"),
	MOTORCYCLE("motos", "Moto"),
	TRUCK("caminhoes", "Caminhao");

	private final String fipePath;
	private final String label;

	VehicleKind(String fipePath, String label) {
		this.fipePath = fipePath;
		this.label = label;
	}

	/** Segmento usado na URL da tabela FIPE. */
	public String getFipePath() {
		return fipePath;
	}

	public String getLabel() {
		return label;
	}
}
