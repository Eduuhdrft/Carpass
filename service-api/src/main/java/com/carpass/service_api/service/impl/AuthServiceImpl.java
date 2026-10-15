package com.carpass.service_api.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.model.PasswordResetToken;
import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.security.AuthenticatedUser;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.JwtService;
import com.carpass.service_api.security.PasswordResetTokenIssuer;
import com.carpass.service_api.service.AuthService;
import com.carpass.service_api.service.EmailService;
import com.carpass.service_api.service.WorkshopService;
import com.carpass.service_api.web.dto.auth.ForgotPasswordRequest;
import com.carpass.service_api.web.dto.auth.LoginRequest;
import com.carpass.service_api.web.dto.auth.LoginResponse;
import com.carpass.service_api.web.dto.auth.RegisterWorkshopRequest;
import com.carpass.service_api.web.dto.auth.ResetPasswordRequest;
import com.carpass.service_api.web.dto.auth.WorkshopOption;
import com.carpass.service_api.web.dto.user.UserResponse;
import com.carpass.service_api.web.dto.workshop.CreateWorkshopRequest;

/**
 * Regras de autenticacao, cadastro inicial e redefinicao de senha.
 */
@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final WorkshopMemberRepository memberRepository;
	private final WorkshopService workshopService;
	private final PasswordResetTokenIssuer tokenIssuer;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;
	private final CurrentUser currentUser;

	public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
			WorkshopMemberRepository memberRepository, WorkshopService workshopService,
			PasswordResetTokenIssuer tokenIssuer, PasswordEncoder passwordEncoder, JwtService jwtService,
			EmailService emailService, CurrentUser currentUser) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.memberRepository = memberRepository;
		this.workshopService = workshopService;
		this.tokenIssuer = tokenIssuer;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.emailService = emailService;
		this.currentUser = currentUser;
	}

	// ------------------------------------------------------------- login
	/**
	 * {@inheritDoc}
	 *
	 * <h2>Tres desfechos, um deles novo</h2>
	 *
	 * <p>
	 * Com um vinculo, o token ja sai escopado e a pessoa entra direto -- o caso
	 * comum, que nao mudou. Com varios, o token sai sem oficina e a lista de
	 * opcoes acompanha, para o cliente escolher. Sem nenhum, a pessoa entra assim
	 * mesmo.
	 * </p>
	 *
	 * <p>
	 * Esse terceiro caso e o que resolve o problema que deu origem a mudanca:
	 * antes, quem era demitido tinha a <em>identidade</em> desativada e ficava
	 * sem conseguir usar o proprio e-mail para nada -- nem para abrir a oficina
	 * dele. Agora ele entra, nao enxerga dado nenhum da ex-oficina (papel nulo
	 * nao casa com expressao de autorizacao alguma) e pode criar a propria.
	 * </p>
	 */
	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());

		// Qualquer falha vira BadCredentialsException e e traduzida para 401 no
		// GlobalExceptionHandler, sem revelar se o e-mail existe.
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		User user = principal.getUser();

		List<WorkshopMember> vinculos = memberRepository.findActiveByUser(user.getId());
		// Um vinculo so entra direto. Com dois, escolher por conta propria seria
		// jogar a pessoa numa oficina que ela talvez nao quisesse abrir agora.
		WorkshopMember escolhido = vinculos.size() == 1 ? vinculos.get(0) : null;

		log.info("Login efetuado: usuario={} vinculos={} oficina={}", user.getId(), vinculos.size(),
				escolhido == null ? null : escolhido.getWorkshop().getId());

		return buildSession(user, escolhido, vinculos);
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponse selectWorkshop(UUID workshopId) {
		User user = userRepository.findById(currentUser.requireId())
				.orElseThrow(() -> ResourceNotFoundException.of("Usuario"));

		// Nao ser membro responde 404, e nao 403: e a mesma regra do resto do
		// sistema -- quem nao pertence a oficina nao deve nem descobrir que ela
		// existe trocando um id.
		WorkshopMember escolhido = memberRepository
				.findByUserIdAndWorkshopIdAndActiveTrue(user.getId(), workshopId)
				.orElseThrow(() -> ResourceNotFoundException.of("Oficina"));

		log.info("Oficina da sessao trocada: usuario={} oficina={}", user.getId(), workshopId);

		return buildSession(user, escolhido, memberRepository.findActiveByUser(user.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkshopOption> myWorkshops() {
		return memberRepository.findActiveByUser(currentUser.requireId()).stream().map(WorkshopOption::from).toList();
	}

	/** Monta token e resposta a partir do vinculo escolhido (que pode ser nulo). */
	private LoginResponse buildSession(User user, WorkshopMember escolhido, List<WorkshopMember> disponiveis) {
		return LoginResponse.of(jwtService.generateToken(user, escolhido), jwtService.getExpirationSeconds(),
				UserResponse.from(user, escolhido), disponiveis.stream().map(WorkshopOption::from).toList());
	}

	// ---------------------------------------------------------- cadastro
	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Continua recusando e-mail ja cadastrado, e agora com uma saida de verdade
	 * para oferecer: quem ja tem conta entra e cria a oficina por dentro, sem
	 * precisar de um segundo endereco. Antes essa recusa era um beco sem saida.
	 * </p>
	 */
	@Override
	@Transactional
	public UserResponse registerWorkshop(RegisterWorkshopRequest request) {
		String email = normalizeEmail(request.email());

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException(
					"Ja existe uma conta com este e-mail. Entre com ela e crie a oficina por dentro do sistema.");
		}

		User owner = new User();
		owner.setFirstName(request.firstName().trim());
		owner.setLastName(request.lastName() == null ? null : request.lastName().trim());
		owner.setEmail(email);
		owner.setPassword(passwordEncoder.encode(request.password()));
		owner.setPhone(request.phone());
		owner.setActive(true);
		userRepository.save(owner);

		WorkshopMember membership = workshopService.createFor(owner, new CreateWorkshopRequest(request.tradeName(),
				request.corporateName(), request.cnpj(), request.workshopPhone(), request.workshopEmail(),
				request.address()));

		log.info("Nova oficina cadastrada pelo cadastro publico: oficina={} proprietario={}",
				membership.getWorkshop().getId(), owner.getId());
		emailService.sendWelcome(owner.getEmail(), owner.getFirstName(), membership.getWorkshop().getTradeName());

		return UserResponse.from(owner, membership);
	}

	// ------------------------------------------------ recuperacao de senha
	@Override
	@Transactional
	public void requestPasswordReset(ForgotPasswordRequest request) {
		Optional<User> found = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()));

		// Resposta identica exista ou nao a conta: evita descobrir e-mails validos.
		if (found.isEmpty() || !found.get().isActive()) {
			log.info("Pedido de redefinicao para e-mail inexistente ou inativo. Nenhum e-mail enviado.");
			return;
		}

		User user = found.get();
		String rawToken = tokenIssuer.issue(user);
		emailService.sendPasswordReset(user.getEmail(), user.getFirstName(), tokenIssuer.buildLink(rawToken),
				tokenIssuer.getValidityMinutes());

		log.info("Token de redefinicao emitido para o usuario {}", user.getId());
	}

	@Override
	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		PasswordResetToken token = tokenIssuer.findUsable(request.token())
				.orElseThrow(() -> new BusinessException("Este link e invalido, expirou ou ja foi utilizado."));

		User user = token.getUser();
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		// Uma conta que define a senha pelo link de primeiro acesso passa a valer.
		user.setActive(true);
		userRepository.save(user);

		tokenIssuer.markUsed(token);

		log.info("Senha definida para o usuario {}", user.getId());
	}

	// --------------------------------------------------------------- apoio
	private String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}
}
