package com.carpass.service_api.service;

import java.util.Map;

/**
 * Envio de e-mails transacionais.
 *
 * <p>
 * As implementacoes nunca devem propagar falha de envio para o fluxo principal:
 * uma indisponibilidade do servidor SMTP nao pode impedir um cadastro.
 * </p>
 */
public interface EmailService {

	/** Boas-vindas apos o cadastro da oficina. */
	void sendWelcome(String to, String name, String workshopName);

	/** Link de redefinicao de senha. */
	void sendPasswordReset(String to, String name, String resetLink, long validityMinutes);

	/** Convite de primeiro acesso para um funcionario recem-cadastrado. */
	void sendAccountInvite(String to, String name, String workshopName, String setPasswordLink, long validityMinutes);

	/**
	 * Aviso de que uma oficina passou a dar acesso a uma conta que ja existia.
	 *
	 * <p>
	 * E o par do convite, para quem ja tem senha. Nao leva link nenhum de
	 * proposito: mandar "defina sua senha" a quem ja tem uma confunde, e o link
	 * daria a oficina que concedeu o acesso o poder de trocar a senha de uma
	 * conta que pode servir tambem a outra oficina.
	 * </p>
	 */
	void sendWorkshopAccessGranted(String to, String name, String workshopName);

	/**
	 * Envia o orcamento ao cliente, com o link para aprovar ou recusar.
	 *
	 * <p>
	 * Recebe um mapa em vez de uma lista de parametros porque o orcamento tem
	 * muitos campos e todos ja chegam formatados como texto -- moeda e rotulo sao
	 * resolvidos no servico, para o template nao precisar saber de locale.
	 * </p>
	 */
	void sendQuoteToCustomer(String to, String subject, Map<String, Object> variables);
}
