package com.carpass.service_api.service.impl;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.FinanceAccess;
import com.carpass.service_api.service.ExpenseService;
import com.carpass.service_api.service.PartTypeService;
import com.carpass.service_api.service.WorkshopService;
import com.carpass.service_api.web.dto.workshop.CreateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.UpdateWorkshopRequest;
import com.carpass.service_api.web.dto.workshop.WorkshopResponse;

/**
 * Cadastro da oficina.
 *
 * <p>
 * A oficina alvo vem sempre do contexto de seguranca, nunca da requisicao, de
 * modo que nao ha como editar a oficina de terceiros.
 * </p>
 */
@Service
public class WorkshopServiceImpl implements WorkshopService {

	private static final Logger log = LoggerFactory.getLogger(WorkshopServiceImpl.class);

	/**
	 * Data URI de imagem: so os formatos que uma folha impressa reproduz bem.
	 *
	 * <p>
	 * SVG fica de fora de proposito -- pode carregar script embutido, e o
	 * navegador que abre a folha o executaria no contexto da aplicacao. Nenhuma
	 * tela pede SVG; nao ha motivo para aceitar um formato que abre essa porta.
	 * </p>
	 */
	private static final Pattern LOGO_DATA_URI = Pattern
			.compile("^data:image/(png|jpeg|jpg|webp);base64,([A-Za-z0-9+/]+=*)$");

	/**
	 * 400 KB decodificados. A logo aparece pequena, no cabecalho da folha
	 * impressa -- nao precisa ser grande, e uma imagem grande so infla a linha
	 * no banco e o payload de cada folha impressa.
	 */
	private static final int MAX_LOGO_BYTES = 400 * 1024;

	private final WorkshopRepository workshopRepository;
	private final WorkshopMemberRepository memberRepository;
	private final UserRepository userRepository;
	private final PartTypeService partTypeService;
	private final ExpenseService expenseService;
	private final FinanceAccess financeAccess;
	private final CurrentUser currentUser;

	public WorkshopServiceImpl(WorkshopRepository workshopRepository, WorkshopMemberRepository memberRepository,
			UserRepository userRepository, PartTypeService partTypeService, ExpenseService expenseService,
			FinanceAccess financeAccess, CurrentUser currentUser) {
		this.workshopRepository = workshopRepository;
		this.memberRepository = memberRepository;
		this.userRepository = userRepository;
		this.partTypeService = partTypeService;
		this.expenseService = expenseService;
		this.financeAccess = financeAccess;
		this.currentUser = currentUser;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * O CNPJ e conferido aqui, e nao so pela restricao do banco, para a pessoa
	 * receber uma mensagem em vez de um erro de integridade.
	 * </p>
	 */
	@Override
	@Transactional
	public WorkshopMember createFor(User owner, CreateWorkshopRequest request) {
		String cnpj = onlyDigits(request.cnpj());
		if (workshopRepository.existsByCnpj(cnpj)) {
			throw new ConflictException("Ja existe uma oficina cadastrada com este CNPJ.");
		}

		Workshop workshop = new Workshop();
		workshop.setTradeName(request.tradeName().trim());
		workshop.setCorporateName(request.corporateName() == null ? null : request.corporateName().trim());
		workshop.setCnpj(cnpj);
		workshop.setPhone(request.workshopPhone());
		workshop.setEmail(request.workshopEmail() == null ? null
				: request.workshopEmail().trim().toLowerCase());
		workshop.setActive(true);
		workshop.setOwner(owner);
		if (request.address() != null) {
			workshop.setAddress(request.address().toAddress());
		}
		workshopRepository.save(workshop);

		WorkshopMember membership = new WorkshopMember();
		membership.setUser(owner);
		membership.setWorkshop(workshop);
		membership.setRole(Role.OWNER);
		membership.setActive(true);
		memberRepository.save(membership);

		// Cadastro vazio e cadastro que ninguem preenche: a oficina ja nasce com a
		// lista de tipos de peca e as categorias de despesa que qualquer uma
		// reconhece, e edita se quiser. Vai na mesma transacao de proposito --
		// oficina sem cadastro nenhum e um estado que o resto do sistema nao espera.
		partTypeService.seedDefaults(workshop);
		expenseService.seedDefaultCategories(workshop);

		log.info("Oficina criada: oficina={} proprietario={}", workshop.getId(), owner.getId());
		return membership;
	}

	@Override
	@Transactional
	public WorkshopResponse createForCurrentUser(CreateWorkshopRequest request) {
		User owner = userRepository.findById(currentUser.requireId())
				.orElseThrow(() -> ResourceNotFoundException.of("Usuario"));

		WorkshopMember membership = createFor(owner, request);
		return WorkshopResponse.from(membership.getWorkshop());
	}

	@Override
	@Transactional(readOnly = true)
	public WorkshopResponse getCurrentWorkshop() {
		return WorkshopResponse.from(loadCurrent());
	}

	@Override
	@Transactional
	public WorkshopResponse updateCurrentWorkshop(UpdateWorkshopRequest request) {
		Workshop workshop = loadCurrent();

		workshop.setTradeName(request.tradeName().trim());
		workshop.setCorporateName(request.corporateName() == null ? null : request.corporateName().trim());
		workshop.setPhone(request.phone());
		workshop.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());
		workshop.setLogoUrl(normalizeLogo(request.logoUrl()));
		workshop.setAddress(request.address() == null ? null : request.address().toAddress());
		// Nulo desliga o encerramento automatico; o valor ja veio validado entre
		// 1 e 720 horas pelo DTO, e o CHECK da tabela cobre qualquer outro caminho.
		workshop.setAutoCloseOpenOrdersHours(request.autoCloseOpenOrdersHours());
		workshopRepository.save(workshop);

		log.info("Dados da oficina atualizados: oficina={}", workshop.getId());
		return WorkshopResponse.from(workshop);
	}

	/**
	 * Liga ou desliga o acesso do gerente ao financeiro.
	 *
	 * <p>
	 * A checagem de dono vive no {@code FinanceAccess}, junto com as outras regras
	 * de permissao financeira -- uma copia dela aqui seria a segunda regra a
	 * manter, e regra de permissao duplicada e onde nasce a brecha.
	 * </p>
	 */
	@Override
	@Transactional
	public WorkshopResponse updateFinanceAccess(boolean managerCanSeeFinance) {
		financeAccess.requireOwner();

		Workshop workshop = loadCurrent();
		workshop.setManagerCanSeeFinance(managerCanSeeFinance);
		workshopRepository.save(workshop);

		log.info("Acesso do gerente ao financeiro alterado: oficina={} permitido={}", workshop.getId(),
				managerCanSeeFinance);
		return WorkshopResponse.from(workshop);
	}

	/**
	 * Valida e normaliza a logo recebida do cadastro.
	 *
	 * <p>
	 * O frontend ja restringe tipo e tamanho do arquivo antes de converter para
	 * base64, mas isso e conveniencia de interface, nao seguranca -- a mesma
	 * checagem precisa acontecer aqui, porque nada obriga quem chama a API a
	 * passar por aquela tela.
	 * </p>
	 *
	 * @return a data URI validada, ou {@code null} quando a oficina removeu a
	 *         logo (campo vazio ou ausente).
	 */
	private String normalizeLogo(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		Matcher matcher = LOGO_DATA_URI.matcher(raw.trim());
		if (!matcher.matches()) {
			throw new BusinessException(
					"Logo em formato invalido. Envie uma imagem PNG, JPG ou WEBP.");
		}

		byte[] decodificado;
		try {
			decodificado = Base64.getDecoder().decode(matcher.group(2));
		} catch (IllegalArgumentException ex) {
			throw new BusinessException("Logo em formato invalido. Envie uma imagem PNG, JPG ou WEBP.");
		}

		if (decodificado.length > MAX_LOGO_BYTES) {
			throw new BusinessException("A logo deve ter no maximo 400 KB.");
		}

		return matcher.group();
	}

	private Workshop loadCurrent() {
		return workshopRepository.findById(currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Oficina"));
	}

	private String onlyDigits(String value) {
		return value == null ? null : value.replaceAll("\\D", "");
	}
}
