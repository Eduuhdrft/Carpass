package com.carpass.service_api.service;

import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.web.dto.workshop.CreateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.UpdateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.WorkshopResponse;

/**
 * Dados cadastrais da oficina do usuario autenticado.
 */
public interface WorkshopService {

	/**
	 * Cria uma oficina e o vinculo de proprietario de quem a criou.
	 *
	 * <p>
	 * Ponto unico dos dois caminhos que criam oficina -- o cadastro publico e a
	 * criacao por quem ja esta logado. Fazer cada um do seu jeito seria ter duas
	 * oficinas com "estado inicial" diferente: uma com os tipos de peca semeados
	 * e a outra sem, por exemplo. O resto do sistema nao espera oficina sem
	 * cadastro nenhum.
	 * </p>
	 *
	 * @return o vinculo de OWNER recem-criado
	 */
	WorkshopMember createFor(User owner, CreateWorkshopRequest request);

	/**
	 * Cria a oficina de quem ja esta autenticado.
	 *
	 * <p>
	 * O caminho de quem saiu de uma oficina e vai abrir a propria. Nao passa por
	 * confirmacao de e-mail porque nao ha o que confirmar: o login ja provou que a
	 * pessoa e dona daquela conta.
	 * </p>
	 */
	WorkshopResponse createForCurrentUser(CreateWorkshopRequest request);

	WorkshopResponse getCurrentWorkshop();

	WorkshopResponse updateCurrentWorkshop(UpdateWorkshopRequest request);

	/**
	 * Define se o gerente enxerga o financeiro.
	 *
	 * <p>
	 * Separado de {@link #updateCurrentWorkshop} de proposito: aquele metodo e
	 * alcancavel pelo gerente, e uma chave de permissao dentro dele seria o
	 * gerente se autorizando ao salvar a propria tela de cadastro.
	 * </p>
	 */
	WorkshopResponse updateFinanceAccess(boolean managerCanSeeFinance);
}
