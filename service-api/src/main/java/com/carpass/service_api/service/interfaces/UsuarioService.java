package com.carpass.service_api.service.interfaces;

import java.util.List;
import java.util.UUID;

import com.carpass.service_api.dto.usuarioDto.SenhaUpdateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioCreateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioUpdateDto;

public interface UsuarioService {

	UsuarioResponseDto salvar(UsuarioCreateDto usuarioDto);
	
	UsuarioResponseDto buscarUsuarioPorId(UUID id);
	
	List<UsuarioResponseDto> buscarTodosUsuarios();
	
	UsuarioResponseDto buscarPorEmail(String email);
	
	UsuarioResponseDto atualizarDados(UsuarioUpdateDto usuarioDto);
	
	void atualizarSenha(SenhaUpdateDto senhaDto);
	
	void deletarUsuario(UUID id);
	
}
