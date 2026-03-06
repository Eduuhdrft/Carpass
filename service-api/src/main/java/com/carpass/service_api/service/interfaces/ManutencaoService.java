package com.carpass.service_api.service.interfaces;

import java.util.List;
import java.util.UUID;

import com.carpass.service_api.dto.manutencaoDto.ManutencaoCreateDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoResponseDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoUpdateDto;

public interface ManutencaoService {

	ManutencaoResponseDto salvar(ManutencaoCreateDto manutencaoDto);
	
	ManutencaoResponseDto buscarManutencaoPorId(UUID id);
	
	List<ManutencaoResponseDto> buscarTodasManutencoes();
	
	ManutencaoResponseDto atualizarDadosManutencao(ManutencaoUpdateDto manutencaoDto);
	
	void deletarManutencao(UUID id);
	
}
