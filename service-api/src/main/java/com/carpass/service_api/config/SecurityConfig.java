package com.carpass.service_api.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.carpass.service_api.security.JwtAuthenticationFilter;
import com.carpass.service_api.security.RestAccessDeniedHandler;
import com.carpass.service_api.security.RestAuthenticationEntryPoint;

/**
 * Configuracao de seguranca da API.
 *
 * <p>
 * Principios adotados:
 * </p>
 * <ul>
 * <li>API stateless: nenhuma sessao no servidor, autenticacao apenas por JWT.</li>
 * <li>Lista de rotas publicas curta e explicita; todo o resto exige autenticacao.</li>
 * <li>Nenhuma rota que altere dados fica publica.</li>
 * <li>CORS restrito a origens configuradas por ambiente.</li>
 * <li>Autorizacao fina por papel fica nos controllers, com {@code @PreAuthorize}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	/** Rotas liberadas sem autenticacao. Manter esta lista minima. */
	private static final String[] PUBLIC_POST_ENDPOINTS = {
			"/api/v1/auth/login",
			"/api/v1/auth/register",
			"/api/v1/auth/password/forgot",
			"/api/v1/auth/password/reset"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;
	private final SecurityProperties securityProperties;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler,
			SecurityProperties securityProperties) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
		this.securityProperties = securityProperties;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// CSRF nao se aplica: a API e stateless e o token nao vai em cookie.
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentTypeOptions(contentType -> {
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						// Orcamento respondido pelo cliente, que nao tem conta. Quem
						// autoriza e o token do link: 32 bytes aleatorios, valido por 24
						// horas e de uso unico. Nenhum id interno entra na URL, e a
						// resposta traz so o que o cliente precisa para decidir.
						.requestMatchers("/api/v1/public/quotes/**").permitAll()
						// O encaminhamento interno para /error nunca deve virar 401: isso
						// trocaria a causa real do erro por uma mensagem de sessao expirada.
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		// Evita revelar, pela mensagem de erro, se o e-mail existe ou nao.
		provider.setHideUserNotFoundExceptions(true);
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(securityProperties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
		config.setExposedHeaders(List.of("Location"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
