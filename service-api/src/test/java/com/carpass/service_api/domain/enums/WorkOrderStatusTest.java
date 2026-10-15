package com.carpass.service_api.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Maquina de estados da ordem de servico.
 *
 * <p>
 * O fluxo da OS e a espinha do sistema. Estes testes travam as regras que nao
 * podem mudar por acidente: nada volta de um estado final, o cancelamento fica
 * disponivel enquanto o veiculo esta na oficina e nenhum estado aponta para si
 * mesmo.
 * </p>
 */
class WorkOrderStatusTest {

	@Test
	@DisplayName("o caminho feliz completo e permitido, passo a passo")
	void happyPathIsAllowed() {
		assertThat(WorkOrderStatus.OPEN.canTransitionTo(WorkOrderStatus.WAITING_DIAGNOSIS)).isTrue();
		assertThat(WorkOrderStatus.WAITING_DIAGNOSIS.canTransitionTo(WorkOrderStatus.IN_DIAGNOSIS)).isTrue();
		assertThat(WorkOrderStatus.IN_DIAGNOSIS.canTransitionTo(WorkOrderStatus.WAITING_APPROVAL)).isTrue();
		assertThat(WorkOrderStatus.WAITING_APPROVAL.canTransitionTo(WorkOrderStatus.APPROVED)).isTrue();
		assertThat(WorkOrderStatus.APPROVED.canTransitionTo(WorkOrderStatus.IN_PROGRESS)).isTrue();
		assertThat(WorkOrderStatus.IN_PROGRESS.canTransitionTo(WorkOrderStatus.FINISHED)).isTrue();
		assertThat(WorkOrderStatus.FINISHED.canTransitionTo(WorkOrderStatus.DELIVERED)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = WorkOrderStatus.class, names = { "DELIVERED", "CANCELED" })
	@DisplayName("estado final nao leva a lugar nenhum")
	void finalStatesHaveNoExit(WorkOrderStatus status) {
		assertThat(status.isFinal()).isTrue();
		assertThat(status.allowedNextStatuses()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = WorkOrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = { "DELIVERED", "CANCELED" })
	@DisplayName("enquanto o veiculo esta na oficina, cancelar sempre e possivel")
	void cancelIsAlwaysReachableBeforeDelivery(WorkOrderStatus status) {
		assertThat(status.canTransitionTo(WorkOrderStatus.CANCELED)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(WorkOrderStatus.class)
	@DisplayName("nenhum estado aponta para si mesmo")
	void noSelfTransition(WorkOrderStatus status) {
		assertThat(status.canTransitionTo(status)).isFalse();
	}

	@Test
	@DisplayName("nao da para pular a aprovacao e entregar direto")
	void cannotSkipStraightToDelivery() {
		assertThat(WorkOrderStatus.OPEN.canTransitionTo(WorkOrderStatus.DELIVERED)).isFalse();
		assertThat(WorkOrderStatus.IN_PROGRESS.canTransitionTo(WorkOrderStatus.DELIVERED)).isFalse();
		assertThat(WorkOrderStatus.WAITING_APPROVAL.canTransitionTo(WorkOrderStatus.IN_PROGRESS)).isFalse();
	}

	@Test
	@DisplayName("uma OS finalizada pode voltar para execucao antes da entrega")
	void finishedCanBeReopened() {
		assertThat(WorkOrderStatus.FINISHED.canTransitionTo(WorkOrderStatus.IN_PROGRESS)).isTrue();
	}

	@Test
	@DisplayName("ordens nao finais contam como abertas")
	void openMeansNotFinal() {
		assertThat(WorkOrderStatus.IN_PROGRESS.isOpen()).isTrue();
		assertThat(WorkOrderStatus.WAITING_PARTS.isOpen()).isTrue();
		assertThat(WorkOrderStatus.DELIVERED.isOpen()).isFalse();
		assertThat(WorkOrderStatus.CANCELED.isOpen()).isFalse();
	}
}
