package com.carpass.service_api.config.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.carpass.service_api.repository.UsuarioRepository;
import com.carpass.service_api.service.interfaces.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UsuarioRepository usuarioRepository;

	public SecurityFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
		this.jwtService = jwtService;
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String path = request.getServletPath();
		
		if(path.startsWith("/auth") || path.startsWith("/api/user/passwordUpdate")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		var token = this.recoverToken(request); // armazena apenas o token puro sem "Bearer" ou valor null
		
		if(token != null) { // se o token existir
			var login = jwtService.validarToken(token); // confere se está tudo certo, se der tudo certo retorna o email(subject) se falhar armazena null
			UserDetails usuario = usuarioRepository.findByEmail(login); // busca o usuario no banco utilizando o email do token
			
			var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()); // pega o usuário, deixa autenticado e da as permissões
			SecurityContextHolder.getContext().setAuthentication(authentication); // grava o usuário, o Spring entende que tem usuário logado nas requisições
		}
		filterChain.doFilter(request, response); // se algo não estiver certo, ele mata a requisição e retorna 403, se estiver certa, deixa a requisição continuar
	}

	private String recoverToken(HttpServletRequest request) {
		var authHeader = request.getHeader("Authorization"); // procura se o header tem "Authorization"
		if (authHeader == null)
			return null; // se não tiver, retorna null
		return authHeader.replace("Bearer ", "").trim(); // Retira a palavra "Bearer" e limpa os espaços para ficar
															// apenas o token
	}
}
