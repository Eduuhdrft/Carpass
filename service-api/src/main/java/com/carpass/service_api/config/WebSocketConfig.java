package com.carpass.service_api.config;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.carpass.service_api.domain.model.User;
import com.carpass.service_api.repository.UserRepository;
import com.carpass.service_api.repository.WorkshopMemberRepository;
import com.carpass.service_api.security.AuthenticatedUser;
import com.carpass.service_api.security.JwtService;

/**
 * Avisos em tempo real.
 *
 * <h2>O risco que esta classe existe para conter</h2>
 *
 * <p>
 * O isolamento entre oficinas e a garantia mais importante do sistema, e um
 * canal aberto e a forma mais facil de furar essa garantia sem ninguem
 * perceber: basta um cliente assinar o topico da oficina errada. Por isso aqui
 * ha duas checagens, nao uma.
 * </p>
 *
 * <ol>
 * <li><strong>CONNECT</strong> exige um JWT valido de usuario ativo. Sem isso a
 * conexao nem se estabelece -- o canal nunca e anonimo.</li>
 * <li><strong>SUBSCRIBE</strong> so aceita o topico da propria oficina, cujo id
 * vem do token e nunca da mensagem. Pedir outro destino derruba a assinatura.</li>
 * </ol>
 *
 * <h2>Por que os eventos nao carregam dados</h2>
 *
 * <p>
 * O que trafega e "algo mudou", nao o conteudo do que mudou. A tela recebe o
 * aviso e busca pela API REST de sempre, que ja e escopada por oficina e
 * respeita papel -- e assim o faturamento continua invisivel para o atendente
 * sem que este canal precise saber disso. Empurrar dados pelo socket obrigaria
 * a reimplementar aqui toda a regra de permissao, e uma copia dessas nasce
 * desatualizada.
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/** Prefixo dos topicos por oficina. O sufixo e sempre o id da oficina. */
	public static final String WORKSHOP_TOPIC_PREFIX = "/topic/workshop/";

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final WorkshopMemberRepository memberRepository;
	private final SecurityProperties securityProperties;

	public WebSocketConfig(JwtService jwtService, UserRepository userRepository,
			WorkshopMemberRepository memberRepository, SecurityProperties securityProperties) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.memberRepository = memberRepository;
		this.securityProperties = securityProperties;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// Broker em memoria: suficiente para uma instancia. Com mais de uma seria
		// preciso um intermediario, senao o aviso so chega a quem esta conectado
		// na mesma instancia que processou a mudanca.
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				// Mesma lista da API: nunca "*".
				.setAllowedOrigins(securityProperties.getAllowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor == null) {
					return message;
				}

				if (StompCommand.CONNECT.equals(accessor.getCommand())) {
					AuthenticatedUser principal = authenticate(accessor)
							.orElseThrow(() -> new IllegalArgumentException("Conexao sem autenticacao valida."));
					accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null,
							principal.getAuthorities()));
					return message;
				}

				if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
					requireOwnWorkshopTopic(accessor);
				}

				return message;
			}
		});
	}

	/** Le o JWT do cabecalho do CONNECT e resolve o usuario. */
	private Optional<AuthenticatedUser> authenticate(StompHeaderAccessor accessor) {
		List<String> headers = accessor.getNativeHeader("Authorization");
		if (headers == null || headers.isEmpty()) {
			return Optional.empty();
		}

		String header = headers.get(0);
		if (header == null || !header.startsWith("Bearer ")) {
			return Optional.empty();
		}

		String token = header.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			return Optional.empty();
		}

		return jwtService.extractClaims(token).flatMap(this::resolvePrincipal);
	}

	/**
	 * Resolve o principal do CONNECT, reconferindo o vinculo contra o banco.
	 *
	 * <p>
	 * Mesma regra do filtro HTTP, e pelo mesmo motivo: o token diz em qual oficina
	 * a pessoa escolheu entrar, mas nao prova que ela ainda pertence a ela. Sem
	 * esta consulta, quem foi demitido continuaria recebendo as notificacoes da
	 * ex-oficina ate o token expirar -- movimentacao de ordem, decisao de
	 * orcamento e estoque baixo, tudo em tempo real.
	 * </p>
	 *
	 * <p>
	 * Token sem oficina nao conecta. Este canal so existe para evento de oficina;
	 * sem vinculo nao ha topico que a pessoa possa assinar, e recusar no CONNECT e
	 * mais honesto do que aceitar a conexao e negar todo SUBSCRIBE depois.
	 * </p>
	 */
	private Optional<AuthenticatedUser> resolvePrincipal(JwtService.TokenClaims claims) {
		if (claims.workshopId() == null) {
			return Optional.empty();
		}

		return userRepository.findByEmailIgnoreCase(claims.email())
				.filter(User::isActive)
				.flatMap(user -> memberRepository
						.findByUserIdAndWorkshopIdAndActiveTrue(user.getId(), claims.workshopId())
						.map(vinculo -> new AuthenticatedUser(user, vinculo)));
	}

	/**
	 * Recusa assinatura de topico de outra oficina.
	 *
	 * <p>
	 * A oficina vem do usuario autenticado no CONNECT, nunca do destino pedido.
	 * Assim, alterar o id na mensagem nao leva a lugar nenhum -- e a mesma regra
	 * da API, onde o {@code workshopId} tambem nunca chega pela requisicao.
	 * </p>
	 */
	private void requireOwnWorkshopTopic(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (destination == null || !destination.startsWith(WORKSHOP_TOPIC_PREFIX)) {
			throw new IllegalArgumentException("Destino nao permitido.");
		}

		Authentication authentication = (Authentication) accessor.getUser();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
			throw new IllegalArgumentException("Assinatura sem autenticacao.");
		}

		UUID workshopId = principal.getWorkshopId();
		if (workshopId == null || !destination.equals(WORKSHOP_TOPIC_PREFIX + workshopId)) {
			throw new IllegalArgumentException("Destino nao permitido.");
		}
	}
}
