package com.carpass.service_api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.security.RoleExpressions;
import com.carpass.service_api.service.FinanceService;
import com.carpass.service_api.web.dto.finance.AnnualFinanceResponse;
import com.carpass.service_api.web.dto.finance.FinancePeriodsResponse;
import com.carpass.service_api.web.dto.finance.MonthlyFinanceResponse;

/**
 * Painel financeiro da oficina.
 *
 * <h2>Duas camadas de permissao, de proposito</h2>
 *
 * <p>
 * O {@code @PreAuthorize} corta atendente e mecanico antes de a requisicao
 * chegar ao servico -- eles nao tem o que fazer aqui em hipotese nenhuma. A
 * segunda camada, no servico, decide entre dono e gerente conforme a opcao que o
 * dono ligou na oficina; essa parte depende do banco e nao caberia numa
 * anotacao sem virar SpEL ilegivel.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/finance")
@PreAuthorize(RoleExpressions.MANAGEMENT)
public class FinanceController {

	private final FinanceService financeService;

	public FinanceController(FinanceService financeService) {
		this.financeService = financeService;
	}

	/**
	 * Financeiro de um mes.
	 *
	 * @param year  ano com quatro digitos
	 * @param month 1 a 12
	 */
	@GetMapping("/monthly")
	public ResponseEntity<MonthlyFinanceResponse> monthly(@RequestParam int year, @RequestParam int month) {
		return ResponseEntity.ok(financeService.monthly(year, month));
	}

	/** Financeiro de um ano, com os doze meses. */
	@GetMapping("/annual")
	public ResponseEntity<AnnualFinanceResponse> annual(@RequestParam int year) {
		return ResponseEntity.ok(financeService.annual(year));
	}

	/**
	 * Periodos que valem oferecer no seletor.
	 *
	 * <p>
	 * A tela pede uma vez ao abrir e nao volta a perguntar enquanto o usuario
	 * navega entre meses -- os periodos disponiveis nao mudam durante a sessao.
	 * </p>
	 */
	@GetMapping("/periods")
	public ResponseEntity<FinancePeriodsResponse> periods() {
		return ResponseEntity.ok(financeService.periods());
	}
}
