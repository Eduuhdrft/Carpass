package com.carpass.service_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartStockMovement;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.repository.PartRepository;
import com.carpass.service_api.repository.PartStockMovementRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.RealtimeNotifier;

/**
 * Regras de saldo de estoque.
 *
 * <p>
 * O ponto sensivel aqui e nunca deixar o estoque ficar negativo: um saldo
 * negativo significa que a oficina acha que tem uma peca que nao existe, e isso
 * se propaga para orcamento e faturamento.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

	@Mock
	private PartRepository partRepository;

	@Mock
	private PartStockMovementRepository movementRepository;

	@Mock
	private CurrentUser currentUser;

	@Mock
	private RealtimeNotifier realtimeNotifier;

	private StockServiceImpl stockService;
	private Part part;

	@BeforeEach
	void setUp() {
		stockService = new StockServiceImpl(partRepository, movementRepository, realtimeNotifier, currentUser);

		part = new Part();
		part.setWorkshop(new Workshop());
		part.setName("Filtro de oleo");
		part.setStockQuantity(10);
		part.setMinimumStock(2);
	}

	@Test
	@DisplayName("entrada soma ao saldo atual")
	void inboundIncreasesStock() {
		when(currentUser.find()).thenReturn(Optional.empty());

		PartStockMovement movement = stockService.register(part, StockMovementType.INBOUND, 5, "compra", null);

		assertThat(part.getStockQuantity()).isEqualTo(15);
		assertThat(movement.getPreviousStock()).isEqualTo(10);
		assertThat(movement.getResultingStock()).isEqualTo(15);
	}

	@Test
	@DisplayName("saida subtrai do saldo atual")
	void outboundDecreasesStock() {
		when(currentUser.find()).thenReturn(Optional.empty());

		stockService.register(part, StockMovementType.OUTBOUND, 4, "uso em OS", null);

		assertThat(part.getStockQuantity()).isEqualTo(6);
	}

	@Test
	@DisplayName("ajuste define o saldo absoluto, e nao um delta")
	void adjustmentSetsAbsoluteValue() {
		when(currentUser.find()).thenReturn(Optional.empty());

		stockService.register(part, StockMovementType.ADJUSTMENT, 3, "contagem fisica", null);

		assertThat(part.getStockQuantity()).isEqualTo(3);
	}

	@Test
	@DisplayName("saida maior que o saldo e recusada e nao altera nada")
	void rejectsOutboundBiggerThanStock() {
		assertThatThrownBy(() -> stockService.register(part, StockMovementType.OUTBOUND, 11, "uso em OS", null))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Estoque insuficiente");

		assertThat(part.getStockQuantity()).isEqualTo(10);
		verify(partRepository, never()).save(any());
		verify(movementRepository, never()).save(any());
	}

	@Test
	@DisplayName("quantidade zero ou negativa e recusada")
	void rejectsNonPositiveQuantity() {
		assertThatThrownBy(() -> stockService.register(part, StockMovementType.INBOUND, 0, null, null))
				.isInstanceOf(BusinessException.class);

		assertThatThrownBy(() -> stockService.register(part, StockMovementType.OUTBOUND, -3, null, null))
				.isInstanceOf(BusinessException.class);

		verify(partRepository, never()).save(any());
	}

	@Test
	@DisplayName("saida que zera o estoque e permitida")
	void allowsOutboundThatEmptiesStock() {
		when(currentUser.find()).thenReturn(Optional.empty());

		stockService.register(part, StockMovementType.OUTBOUND, 10, "uso em OS", null);

		assertThat(part.getStockQuantity()).isZero();
	}

	@Test
	@DisplayName("peca sem saldo definido e tratada como zero")
	void treatsNullStockAsZero() {
		when(currentUser.find()).thenReturn(Optional.empty());
		part.setStockQuantity(null);

		stockService.register(part, StockMovementType.INBOUND, 7, "primeira entrada", null);

		assertThat(part.getStockQuantity()).isEqualTo(7);
	}

	@Test
	@DisplayName("avisa uma vez quando o saldo cruza o minimo, e nao a cada saida seguinte")
	void notifiesOnlyWhenCrossingTheMinimum() {
		// 10 -> 2, com minimo 2: cruzou agora, entao avisa.
		stockService.register(part, StockMovementType.OUTBOUND, 8, null, null);
		verify(realtimeNotifier).lowStock(any(), any(), eq("Filtro de oleo"), eq(2), eq(2));

		// 2 -> 1: ja estava no minimo. Repetir o alerta a cada ordem faria a
		// equipe parar de ler todos eles.
		stockService.register(part, StockMovementType.OUTBOUND, 1, null, null);
		verify(realtimeNotifier, times(1)).lowStock(any(), any(), any(), anyInt(), anyInt());
	}
}
