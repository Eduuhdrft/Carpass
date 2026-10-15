package com.carpass.service_api.web.dto.workorder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Endereco de envio informado no momento de mandar o orcamento.
 *
 * <p>
 * Existe para o caso em que o cliente nao tem e-mail no cadastro, ou em que a
 * proposta precisa ir para outro endereco -- o do escritorio, o do filho, o de
 * quem de fato decide. O cadastro do cliente nao e alterado: o endereco usado
 * fica registrado no proprio token, de modo que o historico diga para onde a
 * proposta realmente foi.
 * </p>
 */
public record QuoteDeliveryRequest(@Email @Size(max = 180) String email) {
}
