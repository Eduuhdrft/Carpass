package com.carpass.service_api.web.dto.common;

import jakarta.validation.constraints.NotNull;

/**
 * Ativa ou desativa um cadastro.
 *
 * <p>
 * Usamos desativacao no lugar de exclusao fisica em tudo que pode aparecer no
 * historico de uma ordem de servico. Apagar de verdade quebraria o passado.
 * </p>
 */
public record ActiveStatusRequest(@NotNull(message = "Informe o novo status") Boolean active) {
}
