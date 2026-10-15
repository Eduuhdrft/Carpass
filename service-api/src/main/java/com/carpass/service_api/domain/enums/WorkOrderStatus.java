package com.carpass.service_api.domain.enums;

import java.util.Set;

/**
 * Estados possiveis de uma ordem de servico.
 *
 * <p>
 * As transicoes permitidas ficam declaradas aqui, junto do proprio estado, e nao
 * espalhadas pelo servico. Isso deixa o fluxo legivel em um lugar so e garante
 * que nenhum caminho novo apareca por descuido: qualquer transicao fora desta
 * lista e recusada.
 * </p>
 */
public enum WorkOrderStatus {

	/** Recem-aberta, ainda sem triagem. */
	OPEN("Aberta"),

	/** Na fila para um mecanico avaliar. */
	WAITING_DIAGNOSIS("Aguardando diagnostico"),

	/** Mecanico avaliando o problema. */
	IN_DIAGNOSIS("Em diagnostico"),

	/** Orcamento enviado, aguardando resposta do cliente. */
	WAITING_APPROVAL("Aguardando aprovacao"),

	/** Cliente aprovou o orcamento; execucao liberada. */
	APPROVED("Aprovada"),

	/** Servico sendo executado. */
	IN_PROGRESS("Em execucao"),

	/** Parada por falta de peca. */
	WAITING_PARTS("Aguardando peca"),

	/** Servico concluido, veiculo pronto para retirada. */
	FINISHED("Finalizada"),

	/** Veiculo entregue ao cliente. Estado final. */
	DELIVERED("Entregue"),

	/** Interrompida antes da conclusao. Estado final. */
	CANCELED("Cancelada");

	private final String label;

	WorkOrderStatus(String label) {
		this.label = label;
	}

	/** Rotulo legivel, usado na interface e em relatorios. */
	public String getLabel() {
		return label;
	}

	/** Estados a partir dos quais a ordem nao se move mais. */
	public boolean isFinal() {
		return this == DELIVERED || this == CANCELED;
	}

	/** Ordens que ainda ocupam a oficina, para efeito de painel e listagens. */
	public boolean isOpen() {
		return !isFinal();
	}

	/**
	 * Estados alcancaveis a partir deste.
	 *
	 * <p>
	 * O cancelamento aparece em quase todos porque a oficina precisa poder
	 * interromper o servico a qualquer momento antes da entrega.
	 * </p>
	 */
	public Set<WorkOrderStatus> allowedNextStatuses() {
		return switch (this) {
			case OPEN -> Set.of(WAITING_DIAGNOSIS, IN_DIAGNOSIS, WAITING_APPROVAL, IN_PROGRESS, CANCELED);
			case WAITING_DIAGNOSIS -> Set.of(IN_DIAGNOSIS, OPEN, CANCELED);
			case IN_DIAGNOSIS -> Set.of(WAITING_APPROVAL, IN_PROGRESS, OPEN, CANCELED);
			case WAITING_APPROVAL -> Set.of(APPROVED, OPEN, CANCELED);
			case APPROVED -> Set.of(IN_PROGRESS, WAITING_PARTS, CANCELED);
			case IN_PROGRESS -> Set.of(WAITING_PARTS, FINISHED, CANCELED);
			case WAITING_PARTS -> Set.of(IN_PROGRESS, FINISHED, CANCELED);
			// Reabrir uma OS finalizada e legitimo: o cliente reclamou antes de retirar.
			case FINISHED -> Set.of(DELIVERED, IN_PROGRESS, CANCELED);
			case DELIVERED, CANCELED -> Set.of();
		};
	}

	/** True se a mudanca para {@code target} e permitida a partir deste estado. */
	public boolean canTransitionTo(WorkOrderStatus target) {
		return allowedNextStatuses().contains(target);
	}
}
