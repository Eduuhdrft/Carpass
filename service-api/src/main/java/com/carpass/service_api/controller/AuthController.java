package com.carpass.service_api.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.carpass.service_api.dto.codigoValidacaoDto.CodigoValidacaoDto;
import com.carpass.service_api.dto.loginDto.LoginRequestDto;
import com.carpass.service_api.dto.loginDto.LoginResponseDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioCreateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.service.interfaces.AuthService;
import com.carpass.service_api.service.interfaces.CodigoValidacaoService;
import com.carpass.service_api.service.interfaces.EmailService;
import com.carpass.service_api.service.interfaces.JwtService;
import com.carpass.service_api.service.interfaces.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	private final AuthenticationManager authenticationManager;
	private final UsuarioService usuarioService;
	private final JwtService jwtService;
	private final EmailService emailService;
	private final AuthService authService;
	private final CodigoValidacaoService codigoValidacaoService;
	
	public AuthController(
							AuthenticationManager authenticationManager,
							UsuarioService usuarioService,
							JwtService jwtService,
							EmailService emailService,
							AuthService authService,
							CodigoValidacaoService codigoValidacaoService
						) {
		this.authenticationManager = authenticationManager;
		this.usuarioService = usuarioService;
		this.jwtService = jwtService;
		this.emailService = emailService;
		this.authService = authService;
		this.codigoValidacaoService = codigoValidacaoService;
	}
	
	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto loginDto) {
		var loginPassword = new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.senha());
		var auth = this.authenticationManager.authenticate(loginPassword);
		
		var token = jwtService.gerarToken((Usuario) auth.getPrincipal());
		
		return ResponseEntity.ok(new LoginResponseDto(token));
	}
	
	@PostMapping("/register")
	public ResponseEntity<UsuarioResponseDto> register(@RequestBody @Valid UsuarioCreateDto registerDto) {
		String encryptedPassword = new BCryptPasswordEncoder().encode(registerDto.senha());
		UsuarioResponseDto usuario = usuarioService.salvar(
				new UsuarioCreateDto(
						registerDto.nome(),
						registerDto.sobrenome(),
						registerDto.email(),
						encryptedPassword,
						registerDto.numero(),
						registerDto.tipoConta()
					)
				);
		
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(usuario.id())
				.toUri();
		
		emailService.enviarEmailRegistro(registerDto.email(), registerDto.nome());
		emailService.recuperarSenhaEmail(registerDto.email(), registerDto.nome());
		return ResponseEntity.created(location).body(usuario);
	}
	
	@GetMapping("/verifyAccount")
	public Integer verificarEmail(@RequestParam("email") String email) {
		Integer codigo = authService.verificarEmail(email);
		
		return codigo;
	}
	
	@PostMapping("/verifyCode")
	public ResponseEntity<UsuarioResponseDto> verificarCodigo(@RequestBody CodigoValidacaoDto codigoDto) {
		UsuarioResponseDto verificandoCodigo = codigoValidacaoService.ValidarCodigo(codigoDto.codigo(), codigoDto.email());
		
		return ResponseEntity.ok(verificandoCodigo);
	}

}

