package com.carpass.service_api.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.enums.Role;
import com.carpass.service_api.domain.model.Employee;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.Workshop;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.EmployeeRepository;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.repository.WorkshopRepository;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.PasswordResetTokenIssuer;
import com.carpass.service_api.service.EmailService;
import com.carpass.service_api.service.EmployeeService;
import com.carpass.service_api.web.dto.common.PageResponse;
import com.carpass.service_api.web.dto.employee.EmployeeRequest;
import com.carpass.service_api.web.dto.employee.EmployeeResponse;
import com.carpass.service_api.web.dto.employee.GrantAccessRequest;

/**
 * Regras de cadastro da equipe.
 *
 * <p>
 * Duas decisoes valem destaque:
 * </p>
 * <ul>
 * <li>Ninguem pode criar um {@code SYSTEM_ADMIN} por aqui. Esse papel pertence a
 * administracao da plataforma, nao a oficina, e permitir isso seria uma escalada
 * de privilegio.</li>
 * <li>A conta de acesso nasce sem senha utilizavel. O funcionario define a dele
 * pelo link de primeiro acesso, entao nem o gerente que criou o cadastro chega a
 * conhecer a senha de outra pessoa.</li>
 * </ul>
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

	private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final WorkshopRepository workshopRepository;
	private final WorkshopMemberRepository memberRepository;
	private final PasswordResetTokenIssuer tokenIssuer;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final CurrentUser currentUser;

	public EmployeeServiceImpl(EmployeeRepository employeeRepository, UserRepository userRepository,
			WorkshopRepository workshopRepository, WorkshopMemberRepository memberRepository,
			PasswordResetTokenIssuer tokenIssuer, PasswordEncoder passwordEncoder, EmailService emailService,
			CurrentUser currentUser) {
		this.employeeRepository = employeeRepository;
		this.userRepository = userRepository;
		this.workshopRepository = workshopRepository;
		this.memberRepository = memberRepository;
		this.tokenIssuer = tokenIssuer;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EmployeeResponse> search(String term, Role role, Boolean active, Pageable pageable) {
		return PageResponse.from(
				employeeRepository.search(currentUser.requireWorkshopId(), normalize(term), role, active, pageable),
				EmployeeResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeResponse> findActiveMechanics() {
		return employeeRepository
				.findByWorkshopIdAndRoleAndActiveTrueOrderByNameAsc(currentUser.requireWorkshopId(), Role.MECHANIC)
				.stream().map(EmployeeResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeResponse findById(UUID id) {
		return EmployeeResponse.from(load(id));
	}

	@Override
	@Transactional
	public EmployeeResponse create(EmployeeRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		validateRole(request.role());

		Employee employee = new Employee();
		employee.setWorkshop(workshopRepository.getReferenceById(workshopId));
		employee.setActive(true);
		apply(request, employee, workshopId);
		employeeRepository.save(employee);

		log.info("Funcionario criado: funcionario={} oficina={}", employee.getId(), workshopId);
		return EmployeeResponse.from(employee);
	}

	@Override
	@Transactional
	public EmployeeResponse update(UUID id, EmployeeRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		Employee employee = load(id);
		validateRole(request.role());

		apply(request, employee, workshopId);
		employeeRepository.save(employee);

		// O papel do funcionario e o do vinculo precisam andar juntos: o vinculo e
		// o que a autorizacao consulta, entao um papel mudado so aqui nao mudaria
		// nada do que a pessoa consegue de fato fazer.
		if (employee.getUser() != null) {
			memberRepository.findByUserIdAndWorkshopId(employee.getUser().getId(), workshopId)
					.filter(vinculo -> vinculo.getRole() != request.role())
					.ifPresent(vinculo -> {
						// Conferido com o papel ANTIGO ainda no lugar: e rebaixar o
						// ultimo dono que deixaria a oficina sem quem administre.
						requireNotLastOwner(vinculo, workshopId);
						vinculo.setRole(request.role());
						memberRepository.save(vinculo);
					});
		}

		log.info("Funcionario atualizado: funcionario={}", id);
		return EmployeeResponse.from(employee);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <h2>Demitir encerra o vinculo, nao a pessoa</h2>
	 *
	 * <p>
	 * Este metodo ja desativou a conta inteira ({@code user.active = false}), e
	 * isso tinha uma consequencia que so aparecia depois: como o e-mail e unico no
	 * sistema, a pessoa demitida nunca mais conseguia usar o proprio endereco --
	 * nem para abrir a oficina dela, nem para voltar a ser contratada aqui. A
	 * ex-empregadora ficava, na pratica, com a chave do e-mail pessoal de alguem.
	 * </p>
	 *
	 * <p>
	 * Agora o que cai e o <strong>vinculo com esta oficina</strong>. O efeito
	 * pratico da demissao e o mesmo -- o acesso morre na hora, porque o filtro
	 * reconfere o vinculo a cada requisicao -- mas a identidade continua de pe.
	 * </p>
	 */
	@Override
	@Transactional
	public EmployeeResponse changeStatus(UUID id, boolean active) {
		UUID workshopId = currentUser.requireWorkshopId();
		Employee employee = load(id);

		employee.setActive(active);

		if (employee.getUser() != null) {
			Optional<WorkshopMember> vinculo = memberRepository
					.findByUserIdAndWorkshopId(employee.getUser().getId(), workshopId);

			if (!active) {
				vinculo.ifPresent(v -> requireNotLastOwner(v, workshopId));
			}

			vinculo.ifPresent(v -> {
				v.setActive(active);
				memberRepository.save(v);
			});
		}

		employeeRepository.save(employee);

		log.info("Funcionario {}: funcionario={} oficina={}", active ? "reativado" : "desativado", id, workshopId);
		return EmployeeResponse.from(employee);
	}

	/**
	 * Impede que a oficina fique sem dono.
	 *
	 * <p>
	 * Sem esta trava, demitir ou rebaixar o unico OWNER deixaria o cadastro da
	 * oficina, o financeiro e a propria gestao de equipe sem ninguem que possa
	 * mexer -- um estado do qual nao ha como sair pela interface.
	 * </p>
	 */
	private void requireNotLastOwner(WorkshopMember vinculo, UUID workshopId) {
		if (vinculo.getRole() != Role.OWNER || !vinculo.isActive()) {
			return;
		}
		if (memberRepository.countByWorkshopIdAndRoleAndActiveTrue(workshopId, Role.OWNER) <= 1) {
			throw new BusinessException(
					"Esta e a unica pessoa com acesso de proprietario. Promova outra antes de tirar o acesso desta.");
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <h2>E-mail que ja existe deixou de ser um beco sem saida</h2>
	 *
	 * <p>
	 * Este metodo recusava qualquer e-mail ja cadastrado. Era a mesma trava da
	 * demissao vista do outro lado: quem ja tinha conta -- inclusive alguem que
	 * esta propria oficina demitiu e quer recontratar -- nao podia receber acesso
	 * com o proprio endereco.
	 * </p>
	 *
	 * <p>
	 * Agora a identidade e <strong>reaproveitada</strong>: a pessoa e a mesma, o
	 * que se cria e um vinculo novo com esta oficina. Quem ja tem senha nao
	 * recebe link de primeiro acesso, so o aviso de que passou a ter acesso aqui
	 * -- mandar "defina sua senha" para quem ja tem uma e um convite a confusao, e
	 * o link daria a esta oficina o poder de trocar a senha de uma conta que
	 * tambem serve a outra.
	 * </p>
	 */
	@Override
	@Transactional
	public EmployeeResponse grantSystemAccess(UUID id, GrantAccessRequest request) {
		UUID workshopId = currentUser.requireWorkshopId();
		Employee employee = load(id);
		String email = request.email().trim().toLowerCase();

		if (employee.getUser() != null) {
			throw new ConflictException("Este funcionario ja possui uma conta de acesso.");
		}
		validateRole(employee.getRole());

		Workshop workshop = workshopRepository.getReferenceById(workshopId);
		Optional<User> existente = userRepository.findByEmailIgnoreCase(email);

		User account = existente.orElseGet(() -> criarContaPorConvite(employee, email));
		boolean contaNova = existente.isEmpty();

		if (!contaNova && memberRepository.existsByUserIdAndWorkshopIdAndActiveTrue(account.getId(), workshopId)) {
			throw new ConflictException("Esta pessoa ja tem acesso a esta oficina.");
		}

		// Recontratar reativa o vinculo antigo em vez de criar um segundo: a
		// restricao de unicidade e (pessoa, oficina), e duas linhas para o mesmo
		// par com papeis diferentes nao teriam resposta certa na autorizacao.
		WorkshopMember vinculo = memberRepository.findByUserIdAndWorkshopId(account.getId(), workshopId)
				.orElseGet(WorkshopMember::new);
		vinculo.setUser(account);
		vinculo.setWorkshop(workshop);
		vinculo.setRole(employee.getRole());
		vinculo.setActive(true);
		memberRepository.save(vinculo);

		employee.setUser(account);
		employee.setEmail(email);
		employeeRepository.save(employee);

		if (contaNova) {
			String rawToken = tokenIssuer.issue(account);
			emailService.sendAccountInvite(email, account.getFirstName(), workshop.getTradeName(),
					tokenIssuer.buildLink(rawToken), tokenIssuer.getValidityMinutes());
		} else {
			emailService.sendWorkshopAccessGranted(email, account.getFirstName(), workshop.getTradeName());
		}

		log.info("Acesso concedido: funcionario={} conta={} oficina={} contaNova={}", id, account.getId(), workshopId,
				contaNova);
		return EmployeeResponse.from(employee);
	}

	/** Conta que ainda nao existe: nasce inativa e so vale quando a senha for definida. */
	private User criarContaPorConvite(Employee employee, String email) {
		User account = new User();
		account.setFirstName(firstNameOf(employee.getName()));
		account.setLastName(lastNameOf(employee.getName()));
		account.setEmail(email);
		// Senha aleatoria e descartada: so vale o que o funcionario definir pelo link.
		account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
		account.setPhone(employee.getPhone());
		account.setActive(false);
		return userRepository.save(account);
	}

	// -------------------------------------------------------------- apoio
	private void apply(EmployeeRequest request, Employee employee, UUID workshopId) {
		employee.setName(request.name().trim());
		employee.setRole(request.role());
		employee.setSpecialty(blankToNull(request.specialty()));
		employee.setPhone(blankToNull(request.phone()));
		employee.setEmail(request.email() == null ? null : request.email().trim().toLowerCase());

		if (request.userId() != null) {
			// A conta so e alcancavel se tiver vinculo ativo com esta oficina --
			// e o mesmo cuidado de sempre: id vindo do cliente nunca alcanca
			// recurso de outra oficina, e o que nao pertence responde 404.
			User user = memberRepository.findByUserIdAndWorkshopIdAndActiveTrue(request.userId(), workshopId)
					.map(WorkshopMember::getUser)
					.orElseThrow(() -> ResourceNotFoundException.of("Conta de acesso"));

			boolean alreadyLinked = employeeRepository.existsByWorkshopIdAndUserId(workshopId, user.getId());
			boolean isSameLink = employee.getUser() != null && employee.getUser().getId().equals(user.getId());
			if (alreadyLinked && !isSameLink) {
				throw new ConflictException("Esta conta de acesso ja esta vinculada a outro funcionario.");
			}
			employee.setUser(user);
		}
	}

	private void validateRole(Role role) {
		if (role == Role.SYSTEM_ADMIN) {
			throw new BusinessException("O papel de administrador do sistema nao pode ser atribuido pela oficina.");
		}
	}

	private Employee load(UUID id) {
		return employeeRepository.findByIdAndWorkshopId(id, currentUser.requireWorkshopId())
				.orElseThrow(() -> ResourceNotFoundException.of("Funcionario"));
	}

	private String firstNameOf(String fullName) {
		return fullName.trim().split("\\s+")[0];
	}

	private String lastNameOf(String fullName) {
		String[] parts = fullName.trim().split("\\s+", 2);
		return parts.length > 1 ? parts[1] : null;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalize(String term) {
		return term == null || term.isBlank() ? null : term.trim();
	}
}
