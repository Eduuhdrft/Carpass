package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.carpass.service_api.dto.usuarioDto.SenhaUpdateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioCreateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioUpdateDto;
import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.exceptions.UsuarioNaoEncontradoException;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.repository.UsuarioRepository;
import com.carpass.service_api.service.interfaces.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

	
	private final UsuarioRepository usuarioRepository;
	
	public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}
	
	// CREATE
	@Override
	public UsuarioResponseDto salvar(UsuarioCreateDto usuarioDto) {
		
		if(usuarioRepository.existsByEmail(usuarioDto.email())) {
			throw new RegraDeNegocioException("Este email já está cadastrado");
		}
		
		if(usuarioRepository.existsByNumero(usuarioDto.numero())) {
			throw new RegraDeNegocioException("Este número já está cadastrado");
		}
		
		String encryptedPassword = new BCryptPasswordEncoder().encode(usuarioDto.senha());
		
		Usuario converterUsuario = new Usuario();
		converterUsuario.setNome(usuarioDto.nome());
		converterUsuario.setSobrenome(usuarioDto.sobrenome());
		converterUsuario.setEmail(usuarioDto.email());
		converterUsuario.setSenha(encryptedPassword);
		converterUsuario.setNumero(usuarioDto.numero());
		converterUsuario.setTipoConta(usuarioDto.tipoConta());
		
		usuarioRepository.save(converterUsuario);
		
		return new UsuarioResponseDto(
				converterUsuario.getId(),
				converterUsuario.getNome(),
				converterUsuario.getSobrenome(),
				converterUsuario.getEmail(),
				converterUsuario.getNumero(),
				converterUsuario.getTipoConta()
			);
	}
	
	// READ BY ID
	@Override
	public UsuarioResponseDto buscarUsuarioPorId(UUID id) {
			Usuario usuario = usuarioRepository.findById(id)
					.orElseThrow(UsuarioNaoEncontradoException::new);
			
			return new UsuarioResponseDto(
					usuario.getId(),
					usuario.getNome(),
					usuario.getSobrenome(),
					usuario.getEmail(),
					usuario.getNumero(),
					usuario.getTipoConta()
				);
		}
	
	// READ ALL
	@Override
	public List<UsuarioResponseDto> buscarTodosUsuarios() {
		return usuarioRepository.findAll() // Retorna uma List<Usuario>
				.stream() // Percorre a lista
				.map( // Converte "usuario" para "usuarioDTO"
					usuario -> new UsuarioResponseDto(
							usuario.getId(),
							usuario.getNome(),
							usuario.getSobrenome(),
							usuario.getEmail(),
							usuario.getNumero(),
							usuario.getTipoConta())).toList(); // Retorna em lista (List<UsuarioResponseDto>)
	}
	
	// READ BY EMAIL
	@Override
	public UsuarioResponseDto buscarPorEmail(String email) {
		Usuario usuario = usuarioRepository.buscarUsuarioPorEmail(email);
		
		return new UsuarioResponseDto(
				usuario.getId(),
				usuario.getNome(),
				usuario.getSobrenome(),
				usuario.getEmail(),
				usuario.getNumero(),
				usuario.getTipoConta()
			);	
		}
	
	// PUT
	@Override
	public UsuarioResponseDto atualizarDados(UsuarioUpdateDto usuarioDto) {
		
		Usuario usuario = usuarioRepository.findById(usuarioDto.id())
				.orElseThrow(UsuarioNaoEncontradoException::new);
		
		if(!usuario.getEmail().equals(usuarioDto.email()) 
				&& usuarioRepository.existsByEmail(usuarioDto.email())) {
			throw new RegraDeNegocioException("Este email já está cadastrado");
		}
		
		if(!usuario.getNumero().equals(usuarioDto.numero()) 
				&& usuarioRepository.existsByNumero(usuarioDto.numero())) {
			throw new RegraDeNegocioException("Este número já está cadastrado");
		}
		
		usuario.setNome(usuarioDto.nome());
		usuario.setSobrenome(usuarioDto.sobrenome());
		usuario.setEmail(usuarioDto.email());
		usuario.setNumero(usuarioDto.numero());
		usuario.setTipoConta(usuarioDto.tipoConta());
		
		usuarioRepository.save(usuario);
		
		return new UsuarioResponseDto(
				usuario.getId(),
				usuario.getNome(),
				usuario.getSobrenome(),
				usuario.getEmail(),
				usuario.getNumero(),
				usuario.getTipoConta()
			);
	}
	
	// PATCH
	@Override
	public void atualizarSenha(SenhaUpdateDto senhaDto) {
		Usuario usuario = usuarioRepository.findById(senhaDto.id())
				.orElseThrow(UsuarioNaoEncontradoException::new);
	
		String encryptedPassword = new BCryptPasswordEncoder().encode(senhaDto.senha());
	
		usuario.setSenha(encryptedPassword);
	
		usuarioRepository.save(usuario);
	}
	
	// DELETE
	@Override
	public void deletarUsuario(UUID id) {
		buscarUsuarioPorId(id);
		usuarioRepository.deleteById(id);
	}
		
}