package com.carpass.service_api.service.interfaces;

import java.util.List;
import java.util.UUID;

import com.carpass.service_api.dto.veiculoDto.VeiculoCreateDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoResponseDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoUpdateDto;

public interface VeiculoService {
	
	VeiculoResponseDto salvar(VeiculoCreateDto veiculoDto);
	
	VeiculoResponseDto buscarVeiculoPorId(UUID id);
	
	List<VeiculoResponseDto> buscarTodosVeiculos();
	
	VeiculoResponseDto atualizarDadosVeiculo(VeiculoUpdateDto veiculoDto);
	
	List<VeiculoResponseDto> buscarPorUsuarioId(UUID id);
	
	void deletarVeiculo(UUID id);
	
}
