package com.carpass.service_api.service;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.models.CodigoValidacao;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.repository.CodigoValidacaoRepository;
import com.carpass.service_api.repository.UsuarioRepository;
import com.carpass.service_api.service.interfaces.AuthService;
import com.carpass.service_api.service.interfaces.EmailService;

@Service
public class AuthServiceImpl implements AuthService {
	
	private final UsuarioRepository usuarioRepository;
	private final EmailService emailService;
	private final CodigoValidacaoRepository codigoValidacaoRepository;
	
	public AuthServiceImpl(UsuarioRepository usuarioRepository, EmailService emailService, CodigoValidacaoRepository codigoValidacaoRepository) {
		this.usuarioRepository = usuarioRepository;
		this.emailService = emailService;
		this.codigoValidacaoRepository = codigoValidacaoRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return usuarioRepository.findByEmail(username);
	}

	@Override
	public Integer verificarEmail(String email) {
		
		boolean emailUser = usuarioRepository.existsByEmail(email);
		
		if(!emailUser) {
			throw new RegraDeNegocioException("Este email não existe");
		}
		
		Usuario usuario = usuarioRepository.buscarUsuarioPorEmail(email);		
		Integer codigo = emailService.recuperarSenhaEmail(usuario.getEmail(), usuario.getNome());
		
		CodigoValidacao salvandoCodigoDb = new CodigoValidacao();
		salvandoCodigoDb.setUsuario(usuario);
		salvandoCodigoDb.setCodigo(codigo);
		salvandoCodigoDb.setDataExpiracao(LocalDateTime.now().plusMinutes(15));
		salvandoCodigoDb.setUsado(false);
		codigoValidacaoRepository.save(salvandoCodigoDb);
		 	
		return codigo;
	}

}
