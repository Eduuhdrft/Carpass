package com.carpass.service_api.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Total gasto em uma categoria dentro de um periodo.
 *
 * @param categoryId   id da categoria
 * @param categoryName nome na hora da consulta -- a categoria e cadastro, e
 *                     renomea-la muda tambem os relatorios antigos
 * @param amount       soma das despesas daquela categoria no periodo
 * @param count        quantos lancamentos
 */
public record CategoryAmount(UUID categoryId, String categoryName, BigDecimal amount, long count) {
}
