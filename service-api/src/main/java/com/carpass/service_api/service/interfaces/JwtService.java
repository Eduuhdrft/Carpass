package com.carpass.service_api.service.interfaces;

import com.carpass.service_api.models.Usuario;

public interface JwtService {
	String gerarToken(Usuario usuario);
	String validarToken(String token);
}
