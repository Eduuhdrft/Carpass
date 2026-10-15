package com.carpass.service_api.web.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carpass.service_api.service.AuthService;
import com.carpass.service_api.web.dto.auth.ForgotPasswordRequest;
import com.carpass.service_api.web.dto.auth.LoginRequest;
import com.carpass.service_api.web.dto.auth.LoginResponse;
import com.carpass.service_api.web.dto.auth.RegisterWorkshopRequest;
import com.carpass.service_api.web.dto.auth.ResetPasswordRequest;
import com.carpass.service_api.web.dto.auth.SelectWorkshopRequest;
import com.carpass.service_api.web.dto.auth.WorkshopOption;
import com.carpass.service_api.web.dto.common.MessageResponse;
import com.carpass.service_api.web.dto.user.UserResponse;

import jakarta.validation.Valid;

/**
 * Rotas publicas de autenticacao.
 *
 * <p>
 * Este e o unico controller com endpoints abertos. Nenhum deles devolve dado de
 * outro usuario nem aceita identificador de recurso vindo do cliente.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/** Autentica e devolve o token de acesso. */
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * Troca a oficina da sessao.
	 *
	 * <p>
	 * Exige estar autenticado, mas nao exige papel nenhum: quem chega aqui pode
	 * estar justamente <em>sem</em> oficina escolhida, logo depois de um login com
	 * varios vinculos. Quem confere que o vinculo existe e esta ativo e o servico.
	 * </p>
	 */
	@PostMapping("/session/workshop")
	public ResponseEntity<LoginResponse> selectWorkshop(@RequestBody @Valid SelectWorkshopRequest request) {
		return ResponseEntity.ok(authService.selectWorkshop(request.workshopId()));
	}

	/** Oficinas em que a pessoa autenticada pode entrar. Alimenta o seletor de troca. */
	@GetMapping("/session/workshops")
	public ResponseEntity<List<WorkshopOption>> myWorkshops() {
		return ResponseEntity.ok(authService.myWorkshops());
	}

	/** Cadastro inicial da oficina junto com o usuario proprietario. */
	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterWorkshopRequest request) {
		UserResponse user = authService.registerWorkshop(request);
		return ResponseEntity.created(URI.create("/api/v1/me")).body(user);
	}

	/**
	 * Solicita o link de redefinicao de senha.
	 *
	 * <p>
	 * Responde sempre 202 com a mesma mensagem, exista ou nao a conta, para nao
	 * permitir descobrir quais e-mails estao cadastrados.
	 * </p>
	 */
	@PostMapping("/password/forgot")
	public ResponseEntity<MessageResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
		authService.requestPasswordReset(request);
		return ResponseEntity.accepted().body(new MessageResponse(
				"Se houver uma conta com este e-mail, enviaremos um link de redefinicao em instantes."));
	}

	/** Conclui a redefinicao usando o token recebido por e-mail. */
	@PostMapping("/password/reset")
	public ResponseEntity<MessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso. Faca login com a nova senha."));
	}
}
