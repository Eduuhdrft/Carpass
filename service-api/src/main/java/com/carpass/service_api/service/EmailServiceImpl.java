package com.carpass.service_api.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.carpass.service_api.service.interfaces.EmailService;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender javaMailSender;
	private final TemplateEngine templateEngine; // Responsável por arquivos html
	
	@Value("${spring.mail.username}")
	private String remetente;
	
	public EmailServiceImpl (JavaMailSender javaMailSender, TemplateEngine templateEngine) {
		this.javaMailSender = javaMailSender;
		this.templateEngine = templateEngine;
	}

	@Override
	public String enviarEmailRegistro(String destinatario, String nome) {
		try {
			Context context = new Context(); // Mapa de dados que será enviado ao template "associar as variaveis que estão la com as daqui"
			context.setVariable("nome", nome);
			
			String html = templateEngine.process("emailRegistro", context); // procura o arquivo html
			
			MimeMessage message = javaMailSender.createMimeMessage(); // Cria a mensagem de email compativel com HTML, anexos e multipart
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8"); // mensagem base, habilita o multipart, evita problemas com acentos e emojis
			
			helper.setTo(destinatario); // quem vai receber o email
			helper.setSubject("Bem-vindo!"); // assunto do email
			helper.setText(html, true); // add econteúdo html ja processado pelo o thymeleaf e o "true" indica que o conteúdo é html
			
			ClassPathResource image = new ClassPathResource("static/carpass-logo.png"); // procura o caminho definido dentro de "src/main/resources
			
			helper.addInline("logo", image); // associa a imagem ao id do html
			
			javaMailSender.send(message); // envia o email
			return "Email enviado com sucesso";
		} catch (Exception e) {
			return "Erro ao tentar enviar email" + e.getLocalizedMessage();
		}
	}

	@Override
	public Integer recuperarSenhaEmail(String destinatario, String nome) {
		try {
			
			Integer codigo = ThreadLocalRandom.current().nextInt(1000, 10000);
			
			Context context = new Context();
			context.setVariable("nome", nome);
			context.setVariable("codigo", codigo);
			
			String html = templateEngine.process("emailRecuperarSenha", context);
			
			MimeMessage message = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			
			helper.setTo(destinatario);
			helper.setSubject("Recuperação de senha");
			helper.setText(html, true);
			
			javaMailSender.send(message);
			return codigo;
		} catch (Exception e) {
			return 0;
		}
	}
}
