package com.carpass.service_api.web.dto.workorder;

import java.time.LocalDateTime;
import java.util.UUID;

import com.carpass.service_api.domain.enums.WorkOrderStatus;
import com.carpass.service_api.domain.model.WorkOrderStatusHistory;

/**
 * Uma transicao de status na linha do tempo da ordem de servico.
 */
public record WorkOrderStatusHistoryResponse(UUID id, WorkOrderStatus fromStatus, String fromStatusLabel,
		WorkOrderStatus toStatus, String toStatusLabel, String changedBy, String note, LocalDateTime changedAt) {

	public static WorkOrderStatusHistoryResponse from(WorkOrderStatusHistory history) {
		return new WorkOrderStatusHistoryResponse(history.getId(), history.getFromStatus(),
				history.getFromStatus() == null ? null : history.getFromStatus().getLabel(), history.getToStatus(),
				history.getToStatus().getLabel(),
				history.getChangedBy() == null ? null : history.getChangedBy().getFullName(), history.getNote(),
				history.getCreatedAt());
	}
}
