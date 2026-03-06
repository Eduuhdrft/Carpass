package com.carpass.service_api.service.interfaces;

import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;

public interface CodigoValidacaoService {
	UsuarioResponseDto ValidarCodigo(Integer codigoUsuario, String email);
}
