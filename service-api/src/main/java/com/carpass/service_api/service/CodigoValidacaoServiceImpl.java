package com.carpass.service_api.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;
import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.models.CodigoValidacao;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.repository.CodigoValidacaoRepository;
import com.carpass.service_api.repository.UsuarioRepository;
import com.carpass.service_api.service.interfaces.CodigoValidacaoService;

@Service
public class CodigoValidacaoServiceImpl implements CodigoValidacaoService {

	private final CodigoValidacaoRepository codigoValidacaoRepository;
	private final UsuarioRepository usuarioRepository;

	public CodigoValidacaoServiceImpl(CodigoValidacaoRepository codigoValidacaoRepository, UsuarioRepository usuarioRepository) {
		this.codigoValidacaoRepository = codigoValidacaoRepository;
		this.usuarioRepository = usuarioRepository;
	}
	
	@Override
	public UsuarioResponseDto ValidarCodigo(Integer codigoUsuario, String email) {
		
		Usuario usuario = usuarioRepository.buscarUsuarioPorEmail(email);
		CodigoValidacao codigoGerado = codigoValidacaoRepository
										.findByUsuario_idAndCodigo(usuario.getId(), codigoUsuario);				
		
		if(!codigoUsuario.equals(codigoGerado.getCodigo())) {
			throw new RegraDeNegocioException("Código inválido");
		}
		if(codigoGerado.isUsado()) {
			throw new RegraDeNegocioException("Este código ja foi usado");
		}
		if(codigoGerado.getDataExpiracao().isBefore(LocalDateTime.now())) {
			throw new RegraDeNegocioException("Código expirado");
		}
		
		codigoGerado.setUsado(true);
		codigoValidacaoRepository.save(codigoGerado);
		
		return new UsuarioResponseDto(
									usuario.getId(),
									usuario.getNome(),
									usuario.getSobrenome(),
									usuario.getEmail(),
									usuario.getNumero(),
									usuario.getTipoConta()
								);
	}
	
}
