package com.carpass.service_api.service.interfaces;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface AuthService extends UserDetailsService {	
	Integer verificarEmail(String email);
}
