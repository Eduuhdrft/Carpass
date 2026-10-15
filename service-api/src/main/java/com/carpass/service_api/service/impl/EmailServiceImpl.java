package com.carpass.service_api.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.carpass.service_api.service.EmailService;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;

/**
 * Envio de e-mails via SMTP com corpo renderizado pelo Thymeleaf.
 *
 * <p>
 * O envio e assincrono e tolerante a falhas: qualquer erro e registrado no log e
 * a operacao de negocio segue normalmente. Quando {@code carpass.mail.enabled} e
 * falso, os e-mails apenas aparecem no log, o que facilita o desenvolvimento
 * local sem SMTP configurado.
 * </p>
 */
@Service
public class EmailServiceImpl implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final TemplateEngine templateEngine;
	private final boolean enabled;
	private final String from;
	private final String smtpUsername;

	public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider, TemplateEngine templateEngine,
			@Value("${carpass.mail.enabled:false}") boolean enabled,
			@Value("${carpass.mail.from:nao-responda@carpass.app}") String from,
			@Value("${spring.mail.username:}") String smtpUsername) {
		this.mailSenderProvider = mailSenderProvider;
		this.templateEngine = templateEngine;
		this.enabled = enabled;
		this.from = from;
		this.smtpUsername = smtpUsername;
	}

	/**
	 * Diz em voz alta, no arranque, o que vai acontecer com os e-mails.
	 *
	 * <p>
	 * Sem isto o modo de log e silencioso demais: a tela diz "convite enviado", o
	 * funcionario nunca recebe nada, e nao ha nenhum sinal de que o envio estava
	 * desligado. Um aviso no arranque custa uma linha e evita exatamente essa
	 * confusao.
	 * </p>
	 */
	@PostConstruct
	void reportConfiguration() {
		if (!enabled) {
			log.warn("Envio de e-mail DESATIVADO (carpass.mail.enabled=false). "
					+ "As mensagens serao apenas registradas no log e ninguem vai receber nada. "
					+ "Para enviar de verdade, defina CARPASS_MAIL_ENABLED=true e as credenciais SMTP.");
			return;
		}

		if (smtpUsername == null || smtpUsername.isBlank()) {
			log.error("carpass.mail.enabled=true mas CARPASS_MAIL_USERNAME esta vazio. "
					+ "A autenticacao SMTP vai falhar e nenhum e-mail sera entregue.");
			return;
		}

		log.info("Envio de e-mail ativo. Remetente: {}", from);
	}

	@Async
	@Override
	public void sendWelcome(String to, String name, String workshopName) {
		Context context = new Context();
		context.setVariable("name", name);
		context.setVariable("workshopName", workshopName);
		send(to, "Bem-vindo ao CarPass", "email/welcome", context);
	}

	@Async
	@Override
	public void sendPasswordReset(String to, String name, String resetLink, long validityMinutes) {
		Context context = new Context();
		context.setVariable("name", name);
		context.setVariable("resetLink", resetLink);
		context.setVariable("validityMinutes", validityMinutes);
		send(to, "Redefinicao de senha - CarPass", "email/password-reset", context);
	}

	@Async
	@Override
	public void sendAccountInvite(String to, String name, String workshopName, String setPasswordLink,
			long validityMinutes) {
		Context context = new Context();
		context.setVariable("name", name);
		context.setVariable("workshopName", workshopName);
		context.setVariable("setPasswordLink", setPasswordLink);
		context.setVariable("validityMinutes", validityMinutes);
		send(to, "Seu acesso ao CarPass", "email/account-invite", context);
	}

	@Async
	@Override
	public void sendWorkshopAccessGranted(String to, String name, String workshopName) {
		Context context = new Context();
		context.setVariable("name", name);
		context.setVariable("workshopName", workshopName);
		send(to, "Voce recebeu acesso a uma oficina no CarPass", "email/workshop-access-granted", context);
	}

	@Async
	@Override
	public void sendQuoteToCustomer(String to, String subject, Map<String, Object> variables) {
		Context context = new Context();
		variables.forEach(context::setVariable);
		send(to, subject, "email/quote", context);
	}

	private void send(String to, String subject, String template, Context context) {
		String html = templateEngine.process(template, context);

		if (!enabled) {
			log.warn("[e-mail DESATIVADO — nada foi entregue] Para: {} | Assunto: {}", to, subject);
			log.debug("[e-mail desativado] Corpo:\n{}", html);
			return;
		}

		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null) {
			log.warn("carpass.mail.enabled=true mas nenhum JavaMailSender foi configurado. E-mail nao enviado.");
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
			log.info("E-mail '{}' enviado para {}", subject, to);
		} catch (Exception ex) {
			// Falha de e-mail nunca deve derrubar a operacao de negocio.
			log.error("Falha ao enviar o e-mail '{}' para {}: {}", subject, to, ex.getMessage());
		}
	}
}
