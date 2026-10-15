package com.carpass.service_api.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.domain.model.WorkshopMember;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.security.JwtService.TokenClaims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Le o token do cabecalho {@code Authorization} e popula o contexto de
 * seguranca.
 *
 * <p>
 * O filtro nunca decide sozinho se a requisicao passa: ele apenas autentica
 * quando ha um token valido. Quem barra o acesso e a cadeia de autorizacao
 * configurada em {@code SecurityConfig}. Token ausente, malformado, expirado ou
 * de usuario inativo simplesmente resulta em contexto vazio, o que produz um
 * 401 limpo em vez de erro 500.
 * </p>
 *
 * <h2>O vinculo e reconferido a cada requisicao</h2>
 *
 * <p>
 * O token diz em qual oficina a pessoa escolheu entrar; ele <strong>nao</strong>
 * prova que ela ainda pertence aquela oficina. A consulta do vinculo acontece
 * aqui, contra o banco, em toda requisicao -- e e isso que faz a demissao valer
 * na hora. Se o papel ou o vinculo estivessem congelados no token, demitir
 * alguem as 9h nao teria efeito nenhum ate o token expirar, e a pessoa
 * continuaria abrindo ordens de servico o dia inteiro.
 * </p>
 *
 * <p>
 * Token que aponta para uma oficina da qual a pessoa nao e mais membro nao vira
 * "autenticado sem oficina": ele <strong>nao autentica</strong>. Rebaixar
 * silenciosamente para um contexto sem vinculo faria a pessoa demitida cair na
 * tela de "criar sua oficina" no meio do expediente, sem entender o que houve;
 * negar produz o 401 que o frontend ja sabe tratar, mandando refazer o login.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final WorkshopMemberRepository memberRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
			WorkshopMemberRepository memberRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.memberRepository = memberRepository;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			resolveToken(request)
					.flatMap(jwtService::extractClaims)
					.flatMap(this::resolvePrincipal)
					.ifPresent(principal -> authenticate(principal, request));
		}

		filterChain.doFilter(request, response);
	}

	private Optional<AuthenticatedUser> resolvePrincipal(TokenClaims claims) {
		Optional<User> found = userRepository.findByEmailIgnoreCase(claims.email()).filter(User::isActive);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		User user = found.get();

		// Sem oficina no token: autenticado, mas sem papel nenhum. Alcanca so o
		// proprio perfil e a criacao da propria oficina.
		if (claims.workshopId() == null) {
			return Optional.of(new AuthenticatedUser(user));
		}

		Optional<WorkshopMember> membership = memberRepository
				.findByUserIdAndWorkshopIdAndActiveTrue(user.getId(), claims.workshopId());

		// Vinculo caiu depois que o token foi emitido: nao autentica.
		return membership.map(vinculo -> new AuthenticatedUser(user, vinculo));
	}

	private void authenticate(AuthenticatedUser principal, HttpServletRequest request) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null,
				principal.getAuthorities());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * Faz o filtro rodar tambem no dispatch de erro.
	 *
	 * <p>
	 * O padrao do {@link OncePerRequestFilter} e pular o encaminhamento para
	 * {@code /error}. Com isso, qualquer 404 ou erro de servidor em uma requisicao
	 * autenticada chegava ao {@code /error} sem contexto de seguranca e voltava
	 * como 401 "sessao expirada", escondendo a causa real do problema.
	 * </p>
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	private Optional<String> resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HEADER);
		if (header == null || !header.startsWith(PREFIX)) {
			return Optional.empty();
		}
		String token = header.substring(PREFIX.length()).trim();
		return token.isEmpty() ? Optional.empty() : Optional.of(token);
	}
}
