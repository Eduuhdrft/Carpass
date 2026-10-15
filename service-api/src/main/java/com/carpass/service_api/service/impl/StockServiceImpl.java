package com.carpass.service_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.StockMovementType;
import com.carpass.service_api.domain.model.Part;
import com.carpass.service_api.domain.model.PartStockMovement;
import com.carpass.service_api.domain.model.WorkOrder;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.repository.PartRepository;
import com.carpass.service_api.repository.PartStockMovementRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.service.RealtimeNotifier;
import com.carpass.service_api.service.StockService;

/**
 * Controle de saldo de estoque.
 */
@Service
public class StockServiceImpl implements StockService {

	private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

	private final PartRepository partRepository;
	private final PartStockMovementRepository movementRepository;
	private final CurrentUser currentUser;
	private final RealtimeNotifier realtimeNotifier;

	public StockServiceImpl(PartRepository partRepository, PartStockMovementRepository movementRepository,
			RealtimeNotifier realtimeNotifier,
			CurrentUser currentUser) {
		this.partRepository = partRepository;
		this.movementRepository = movementRepository;
		this.currentUser = currentUser;
		this.realtimeNotifier = realtimeNotifier;
	}

	@Override
	@Transactional
	public PartStockMovement register(Part part, StockMovementType type, int quantity, String reason,
			WorkOrder workOrder) {

		if (quantity <= 0) {
			throw new BusinessException("A quantidade da movimentacao deve ser maior que zero.");
		}

		int previous = part.getStockQuantity() == null ? 0 : part.getStockQuantity();
		int resulting = switch (type) {
			case INBOUND -> previous + quantity;
			case OUTBOUND -> previous - quantity;
			// No ajuste a quantidade informada e o novo saldo, nao um delta.
			case ADJUSTMENT -> quantity;
		};

		if (resulting < 0) {
			throw new BusinessException("Estoque insuficiente para a peca \"" + part.getName() + "\". Disponivel: "
					+ previous + ", solicitado: " + quantity + ".");
		}

		part.setStockQuantity(resulting);
		partRepository.save(part);

		PartStockMovement movement = new PartStockMovement();
		movement.setWorkshop(part.getWorkshop());
		movement.setPart(part);
		movement.setWorkOrder(workOrder);
		movement.setType(type);
		movement.setQuantity(quantity);
		movement.setPreviousStock(previous);
		movement.setResultingStock(resulting);
		movement.setReason(reason);
		movement.setCreatedBy(currentUser.find().map(principal -> principal.getUser()).orElse(null));
		movementRepository.save(movement);

		log.info("Estoque movimentado: peca={} tipo={} qtd={} saldo {} -> {}", part.getId(), type, quantity, previous,
				resulting);

		notifyIfLowStock(part, previous, resulting);

		return movement;
	}

	/**
	 * Avisa quando a peca cruza o minimo, e so quando cruza.
	 *
	 * <p>
	 * Comparar com o saldo anterior evita repetir o aviso a cada saida de uma
	 * peca que ja estava abaixo do minimo -- caso contrario a equipe receberia o
	 * mesmo alerta a cada ordem aberta e pararia de ler todos eles.
	 * </p>
	 */
	private void notifyIfLowStock(Part part, int previous, int resulting) {
		int minimum = part.getMinimumStock() == null ? 0 : part.getMinimumStock();
		boolean estavaOk = previous > minimum;
		boolean ficouBaixo = resulting <= minimum;

		if (estavaOk && ficouBaixo) {
			realtimeNotifier.lowStock(part.getWorkshop().getId(), part.getId(), part.getName(), resulting, minimum);
		}
	}
}
