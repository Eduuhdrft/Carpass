package com.carpass.service_api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.carpass.service_api.dto.veiculoDto.VeiculoCreateDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoResponseDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoUpdateDto;
import com.carpass.service_api.service.interfaces.VeiculoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/car") // http://localhost:8080/api/car
public class VeiculoController {

	private final VeiculoService veiculoService;
	
	public VeiculoController(VeiculoService veiculoService) {
		this.veiculoService = veiculoService;
	}
	
	@PostMapping // POST
	public ResponseEntity<VeiculoResponseDto> salvar(@RequestBody @Valid VeiculoCreateDto veiculoDto) {
		VeiculoResponseDto veiculo = veiculoService.salvar(veiculoDto);
		
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(veiculo.id())
				.toUri();
		
		return ResponseEntity.created(location).body(veiculo);
	}
	
	@GetMapping("/{id}") // GET BY ID
	public ResponseEntity<VeiculoResponseDto> buscarVeiculoPorId(@PathVariable("id") UUID id) {
		VeiculoResponseDto veiculo = veiculoService.buscarVeiculoPorId(id);
		return ResponseEntity.ok(veiculo);
	}
	
	@GetMapping("/all") // GET ALL
	public ResponseEntity<List<VeiculoResponseDto>> buscarTodosVeiculos() {
		List<VeiculoResponseDto> veiculos = veiculoService.buscarTodosVeiculos();
		return ResponseEntity.ok(veiculos);
	}
	
	@GetMapping("/usuario/{id}") // GET BY USUARIO_ID
	public ResponseEntity<List<VeiculoResponseDto>> buscarTodosVeiculosUsuarioId(@PathVariable("id") UUID id) {
		List<VeiculoResponseDto> veiculos = veiculoService.buscarPorUsuarioId(id);
		return ResponseEntity.ok(veiculos);
	}
	
	@PutMapping("/carUpdate") // PUT
	public ResponseEntity<VeiculoResponseDto> atualizarDadosVeiculo(@RequestBody @Valid VeiculoUpdateDto veiculoDto) {
		VeiculoResponseDto veiculo = veiculoService.atualizarDadosVeiculo(veiculoDto);
		return ResponseEntity.ok(veiculo);
	}
	
	@DeleteMapping("/carDelete/{id}") // DELETE
	public ResponseEntity<Void> deletarVeiculo(@PathVariable("id") UUID id) {
		veiculoService.deletarVeiculo(id);
		return ResponseEntity.noContent().build();
	}
	
}
