package com.carpass.service_api.web.dto.workorder;

import jakarta.validation.constraints.Size;

/**
 * Observacao opcional que acompanha uma acao, como cancelar ou recusar um
 * orcamento. Fica registrada na linha do tempo da ordem de servico.
 */
public record NoteRequest(@Size(max = 500) String note) {
}
