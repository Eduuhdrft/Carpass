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

import com.carpass.service_api.dto.manutencaoDto.ManutencaoCreateDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoResponseDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoUpdateDto;
import com.carpass.service_api.service.interfaces.ManutencaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/carRepair") // http://localhost:8080/api/carRepair
public class ManutencaoController {

	private final ManutencaoService manutencaoService;
	
	public ManutencaoController(ManutencaoService manutencaoService) {
		this.manutencaoService = manutencaoService;
	}
	
	@PostMapping // POST http://localhost:8080/api/carRepair
	public ResponseEntity<ManutencaoResponseDto> salvar(@RequestBody @Valid ManutencaoCreateDto manutencaoDto) {
		ManutencaoResponseDto manutencao = manutencaoService.salvar(manutencaoDto);
		
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(manutencao.id())
				.toUri();
		
		return ResponseEntity.created(location).body(manutencao);
	}
	
	@GetMapping("/{id}") // GET BY ID
	public ResponseEntity<ManutencaoResponseDto> buscarManutencaoPorId(@PathVariable("id") UUID id) {
		ManutencaoResponseDto manutencao = manutencaoService.buscarManutencaoPorId(id);
		return ResponseEntity.ok(manutencao);
	}
	
	@GetMapping("/all") // GET ALL
	public ResponseEntity<List<ManutencaoResponseDto>> buscarTodasManutencoes() {
		List<ManutencaoResponseDto> manutencoes = manutencaoService.buscarTodasManutencoes();
		return ResponseEntity.ok(manutencoes);
	}
	
	@PutMapping("/updateCarRepair") // UPDATE
	public ResponseEntity<ManutencaoResponseDto> atualizarDadosManutencao(@RequestBody @Valid ManutencaoUpdateDto manutencaoDto) {
		ManutencaoResponseDto manutencao = manutencaoService.atualizarDadosManutencao(manutencaoDto);
		return ResponseEntity.ok(manutencao);
	}
	
	@DeleteMapping("/deleteCarRepair/{id}") // DELETE
	public ResponseEntity<Void> deletarManutencao(@PathVariable("id") UUID id) {
		manutencaoService.deletarManutencao(id);
		return ResponseEntity.noContent().build();
	}
	
}
