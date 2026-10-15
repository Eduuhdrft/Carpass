package com.carpass.service_api.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.carpass.service_api.repository.UserRepository;

/**
 * Carrega o usuario para o fluxo de login por e-mail e senha.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public AppUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmailIgnoreCase(email)
				.map(AuthenticatedUser::new)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
	}
}
