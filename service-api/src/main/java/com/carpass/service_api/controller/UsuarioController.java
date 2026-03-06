package com.carpass.service_api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.carpass.service_api.dto.usuarioDto.SenhaUpdateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioCreateDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioResponseDto;
import com.carpass.service_api.dto.usuarioDto.UsuarioUpdateDto;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.service.interfaces.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user") // http://localhost:8080/api/user
public class UsuarioController {
	
	private final UsuarioService usuarioService;
	
	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService =  usuarioService;
	}
	
	@PostMapping // POST
	public ResponseEntity<UsuarioResponseDto> salvar(@RequestBody @Valid UsuarioCreateDto user) {
		UsuarioResponseDto usuario = usuarioService.salvar(user);
		
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(usuario.id())
				.toUri();
		
		return ResponseEntity.created(location).body(usuario);
	}
	
	@GetMapping("/{id}") // GET BY ID
	public ResponseEntity<UsuarioResponseDto> buscarPorId(@PathVariable("id") UUID id) {
		UsuarioResponseDto usuario = usuarioService.buscarUsuarioPorId(id);
		return ResponseEntity.ok(usuario);
	}
	
	@GetMapping("/all") // GET ALL
	public ResponseEntity<List<UsuarioResponseDto>> buscarTodosUsuarios() {
		List<UsuarioResponseDto> usuarios = usuarioService.buscarTodosUsuarios();
		return ResponseEntity.ok(usuarios);
	}
	
	@GetMapping("/me") // GET BY EMAIL
	public ResponseEntity<UsuarioResponseDto> getUsuarioLogado(@AuthenticationPrincipal Usuario usuario) {
		
		return ResponseEntity.ok(new UsuarioResponseDto(
									usuario.getId(),
									usuario.getNome(),
									usuario.getSobrenome(),
									usuario.getEmail(),
									usuario.getNumero(),
									usuario.getTipoConta()
								)
						);
	}
	
	@PutMapping("/userUpdate") // PUT
	public ResponseEntity<UsuarioResponseDto> atualizarUsuario(@RequestBody @Valid UsuarioUpdateDto user) {
		UsuarioResponseDto usuario = usuarioService.atualizarDados(user);
		return ResponseEntity.ok(usuario);
	}
	
	@PatchMapping("/passwordUpdate") // PATCH
	public ResponseEntity<Void> atualizarSenha(@RequestBody @Valid SenhaUpdateDto senhaDto) {
		usuarioService.atualizarSenha(senhaDto);
		return ResponseEntity.noContent().build();
	}
	
	@DeleteMapping("/userDelete/{id}") // DELETE
	public ResponseEntity<Void> deletar(@PathVariable("id") UUID id) {
		usuarioService.deletarUsuario(id);
		return ResponseEntity.noContent().build();
	}
	
}
