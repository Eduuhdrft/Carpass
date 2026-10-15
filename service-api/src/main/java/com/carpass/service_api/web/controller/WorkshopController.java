package com.carpass.service_api.web.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.service.WorkshopService;
import com.carpass.service_api.web.dto.workshop.CreateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.FinanceAccessRequest;
import com.carpass.service_api.web.dto.workshop.UpdateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.WorkshopResponse;

import jakarta.validation.Valid;

/**
 * Cadastro da oficina do usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/workshop")
public class WorkshopController {

	private final WorkshopService workshopService;

	public WorkshopController(WorkshopService workshopService) {
		this.workshopService = workshopService;
	}

	/**
	 * Cria uma oficina para quem ja esta autenticado.
	 *
	 * <h2>Por que este endpoint nao exige papel nenhum</h2>
	 *
	 * <p>
	 * Ele existe justamente para quem <strong>nao tem</strong> papel: a pessoa que
	 * foi desligada de uma oficina e quer abrir a dela. Exigir papel aqui seria
	 * reproduzir, com outra roupagem, o beco sem saida que esta mudanca veio
	 * desfazer -- so que agora dentro do sistema em vez do cadastro.
	 * </p>
	 *
	 * <p>
	 * Nao ha confirmacao por e-mail neste caminho porque nao ha o que confirmar: o
	 * login ja provou que a pessoa e dona daquela conta. A conferencia de e-mail
	 * pertence ao cadastro publico, onde ninguem provou nada ainda.
	 * </p>
	 */
	@PostMapping
	public ResponseEntity<WorkshopResponse> createWorkshop(@RequestBody @Valid CreateWorkshopRequest request) {
		WorkshopResponse criada = workshopService.createForCurrentUser(request);
		return ResponseEntity.created(URI.create("/api/v1/workshop")).body(criada);
	}

	/** Qualquer pessoa da equipe pode consultar os dados da propria oficina. */
	@GetMapping
	public ResponseEntity<WorkshopResponse> getWorkshop() {
		return ResponseEntity.ok(workshopService.getCurrentWorkshop());
	}

	/** Apenas proprietario e gerente alteram o cadastro da oficina. */
	@PutMapping
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
	public ResponseEntity<WorkshopResponse> updateWorkshop(@RequestBody @Valid UpdateWorkshopRequest request) {
		return ResponseEntity.ok(workshopService.updateCurrentWorkshop(request));
	}

	/**
	 * Define se o gerente enxerga o financeiro.
	 *
	 * <p>
	 * Endpoint separado do cadastro porque o cadastro e editavel pelo gerente. Se
	 * esta chave viajasse dentro do {@code PUT} acima, o gerente se concederia
	 * acesso ao financeiro salvando a propria tela. Quem barra de fato e o
	 * servico; a anotacao aqui so evita a viagem inutil.
	 * </p>
	 */
	@PatchMapping("/finance-access")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<WorkshopResponse> updateFinanceAccess(@RequestBody @Valid FinanceAccessRequest request) {
		return ResponseEntity.ok(workshopService.updateFinanceAccess(request.managerCanSeeFinance()));
	}
}
