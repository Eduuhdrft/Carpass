package com.carpass.service_api.service.interfaces;

public interface EmailService {
	String enviarEmailRegistro(String destinatario, String nome);
	Integer recuperarSenhaEmail(String destinatario, String nome);
}
