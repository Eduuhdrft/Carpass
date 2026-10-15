package com.carpass.service_api.repository.projection;

/**
 * Uma marca presente no estoque da oficina.
 *
 * @param manufacturer  nome da marca como esta cadastrado nas pecas
 * @param partCount     quantas pecas ativas sao dessa marca
 * @param stockQuantity soma do saldo dessas pecas
 */
public record ManufacturerSummary(String manufacturer, long partCount, long stockQuantity) {
}
