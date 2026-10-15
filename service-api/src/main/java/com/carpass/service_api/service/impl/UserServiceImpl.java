package com.carpass.service_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.exception.BusinessException;
import com.carpass.service_api.exception.ConflictException;
import com.carpass.service_api.exception.ResourceNotFoundException;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.security.AuthenticatedUser;
import com.carpass.service_api.security.CurrentUser;
import com.carpass.service_api.security.PasswordResetTokenIssuer;
import com.carpass.service_api.service.UserService;
import com.carpass.service_api.web.dto.auth.ChangePasswordRequest;
import com.carpass.service_api.web.dto.user.UpdateProfileRequest;
import com.carpass.service_api.web.dto.user.UserResponse;

/**
 * Conta do usuario autenticado.
 *
 * <p>
 * Todas as operacoes agem sobre o usuario do contexto de seguranca. Nenhum
 * identificador vindo do cliente e aceito, o que elimina a possibilidade de
 * alterar a conta de outra pessoa trocando um id na requisicao.
 * </p>
 */
@Service
public class UserServiceImpl implements UserService {

	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	private final UserRepository userRepository;
	private final PasswordResetTokenIssuer tokenIssuer;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUser currentUser;

	public UserServiceImpl(UserRepository userRepository, PasswordResetTokenIssuer tokenIssuer,
			PasswordEncoder passwordEncoder, CurrentUser currentUser) {
		this.userRepository = userRepository;
		this.tokenIssuer = tokenIssuer;
		this.passwordEncoder = passwordEncoder;
		this.currentUser = currentUser;
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser() {
		return UserResponse.from(loadCurrent(), currentMembership());
	}

	@Override
	@Transactional
	public UserResponse updateProfile(UpdateProfileRequest request) {
		User user = loadCurrent();
		String email = request.email().trim().toLowerCase();

		if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
			throw new ConflictException("Ja existe uma conta com este e-mail.");
		}

		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName() == null ? null : request.lastName().trim());
		user.setEmail(email);
		user.setPhone(request.phone());
		userRepository.save(user);

		log.info("Perfil atualizado: usuario={}", user.getId());
		return UserResponse.from(user, currentMembership());
	}

	@Override
	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		User user = loadCurrent();

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException("A senha atual esta incorreta.");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
			throw new BusinessException("A nova senha precisa ser diferente da atual.");
		}

		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		// Trocar a senha invalida qualquer link de redefinicao em aberto.
		tokenIssuer.invalidateAll(user);

		log.info("Senha alterada pelo proprio usuario: usuario={}", user.getId());
	}

	/** Recarrega o usuario do banco para trabalhar sempre com uma entidade gerenciada. */
	private User loadCurrent() {
		return userRepository.findById(currentUser.requireId())
				.orElseThrow(() -> ResourceNotFoundException.of("Usuario"));
	}

	/**
	 * O vinculo da sessao, ou nulo quando a pessoa esta fora de qualquer oficina.
	 *
	 * <p>
	 * Vem do principal, e nao de uma consulta nova: o filtro ja conferiu contra o
	 * banco que este vinculo existe e esta ativo nesta requisicao. Consultar de
	 * novo aqui so acrescentaria a chance de as duas leituras discordarem.
	 * </p>
	 */
	private WorkshopMember currentMembership() {
		return currentUser.find().map(AuthenticatedUser::getMembership).orElse(null);
	}
}
