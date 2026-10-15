package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import com.carpass.service_api.web.dto.auth.ForgotPasswordRequest;
import com.carpass.service_api.web.dto.auth.LoginRequest;
import com.carpass.service_api.web.dto.auth.LoginResponse;
import com.carpass.service_api.web.dto.auth.RegisterWorkshopRequest;
import com.carpass.service_api.web.dto.auth.ResetPasswordRequest;
import com.carpass.service_api.web.dto.auth.WorkshopOption;
import com.carpass.service_api.web.dto.user.UserResponse;

/**
 * Autenticacao e ciclo de vida das credenciais.
 */
public interface AuthService {

	LoginResponse login(LoginRequest request);

	/**
	 * Troca a oficina da sessao, devolvendo um token escopado na escolhida.
	 *
	 * <p>
	 * O vinculo e conferido aqui contra o usuario autenticado. Pedir uma oficina
	 * de que a pessoa nao e membro responde 404, e nao 403 -- a mesma regra do
	 * resto do sistema: quem nao pertence a oficina nao deve nem descobrir que ela
	 * existe.
	 * </p>
	 */
	LoginResponse selectWorkshop(UUID workshopId);

	/** Oficinas em que a pessoa autenticada pode entrar, para o seletor de troca. */
	List<WorkshopOption> myWorkshops();

	/** Cria a oficina e o usuario proprietario. */
	UserResponse registerWorkshop(RegisterWorkshopRequest request);

	/** Dispara o e-mail com o link de redefinicao, se o e-mail existir. */
	void requestPasswordReset(ForgotPasswordRequest request);

	/** Consome o token e grava a nova senha. */
	void resetPassword(ResetPasswordRequest request);
}
