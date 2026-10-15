package com.carpass.service_api.web.dto.workorder;

import com.carpass.service_api.domain.enums.WorkOrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Mudanca de status da ordem de servico.
 *
 * @param note observacao livre, gravada na linha do tempo da OS
 */
public record StatusChangeRequest(

		@NotNull(message = "Informe o novo status")
		WorkOrderStatus status,

		@Size(max = 500)
		String note) {
}
