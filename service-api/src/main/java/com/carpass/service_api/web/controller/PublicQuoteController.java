package com.carpass.service_api.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.service.WorkOrderService;
import com.carpass.service_api.web.dto.workorder.NoteRequest;
import com.carpass.service_api.web.dto.workorder.PublicQuoteResponse;

import jakarta.validation.Valid;

/**
 * Orcamento visto e respondido pelo cliente, sem conta no sistema.
 *
 * <p>
 * Estas sao as unicas rotas de dados abertas da aplicacao, entao vale registrar
 * o que as mantem seguras:
 * </p>
 *
 * <ul>
 * <li>O token e a autorizacao. Ele nasce de 32 bytes aleatorios, so o hash fica
 * no banco, vale 24 horas e serve uma vez.</li>
 * <li>Nenhum identificador interno entra na URL. O cliente nunca informa qual
 * ordem quer ver -- o token e que aponta para ela, e para uma so.</li>
 * <li>A resposta e um DTO enxuto: dados do carro, itens e valores. Nada de
 * diagnostico tecnico, observacao interna ou dado de outro cliente.</li>
 * <li>Token invalido, expirado e ja usado devolvem a mesma mensagem, para a
 * rota nao virar um verificador de tokens validos.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/public/quotes")
public class PublicQuoteController {

	private final WorkOrderService workOrderService;

	public PublicQuoteController(WorkOrderService workOrderService) {
		this.workOrderService = workOrderService;
	}

	/** Mostra o orcamento ao cliente. */
	@GetMapping("/{token}")
	public ResponseEntity<PublicQuoteResponse> find(@PathVariable String token) {
		return ResponseEntity.ok(workOrderService.findQuoteByToken(token));
	}

	/** O cliente aprova. */
	@PostMapping("/{token}/approve")
	public ResponseEntity<PublicQuoteResponse> approve(@PathVariable String token) {
		return ResponseEntity.ok(workOrderService.approveQuoteByToken(token));
	}

	/** O cliente recusa, podendo dizer o motivo. */
	@PostMapping("/{token}/reject")
	public ResponseEntity<PublicQuoteResponse> reject(@PathVariable String token,
			@RequestBody(required = false) @Valid NoteRequest request) {
		return ResponseEntity.ok(workOrderService.rejectQuoteByToken(token, request == null ? null : request.note()));
	}
}
