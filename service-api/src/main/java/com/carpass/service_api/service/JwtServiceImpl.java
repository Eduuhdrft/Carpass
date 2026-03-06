package com.carpass.service_api.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.service.interfaces.JwtService;

@Service
public class JwtServiceImpl implements JwtService {

	@Value("${api.security.token.secret}") // inserindo o valor que esta no properties
	private String SECRET; // Pegando a variavel de ambiente no properties
	private final long EXPIRACAO = 86400000; // tempo de expiração em milissegundos = 1 dia
	
	@Override
	public String gerarToken(Usuario usuario) {
		
		return JWT.create() // inicia o builder do JWT
				.withIssuer("carpass-api") // quem emitiu 
				.withSubject(usuario.getEmail()) // quem é o dono do token (o email apontado para o usuário)
				.withIssuedAt(new Date()) // quando o token foi criado
				.withExpiresAt(new Date(System.currentTimeMillis() + EXPIRACAO)) // quando o token expira (data de hoje + 1 dia)
				.sign(Algorithm.HMAC256(SECRET)); // gera o token válido assinado
	}

	@Override
	public String validarToken(String token) {
		try {
			Algorithm algorithm = Algorithm.HMAC256(SECRET); // cria o algoritmo
			
			return JWT.require(algorithm) // verifica token (só aceita token assinado com o algoritmo e a chave)
					.withIssuer("carpass-api") // o token deve conter "carpass-api" se não cai como inválido
					.build() // finaliza a verificação
					.verify(token) // confere se tudo está correto
					.getSubject(); // pega o payload e retorna o subject(email)
		} catch (JWTVerificationException e) {
			return null; // se algo der errado, retornal null e da erro 403
		}
	}

}
