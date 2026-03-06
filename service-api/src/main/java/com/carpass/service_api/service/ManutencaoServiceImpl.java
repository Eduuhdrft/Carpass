package com.carpass.service_api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.carpass.service_api.dto.manutencaoDto.ManutencaoCreateDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoResponseDto;
import com.carpass.service_api.dto.manutencaoDto.ManutencaoUpdateDto;
import com.carpass.service_api.exceptions.ManutencaoNaoEncontradaException;
import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.exceptions.VeiculoNaoEncontradoException;
import com.carpass.service_api.models.Manutencao;
import com.carpass.service_api.models.Veiculo;
import com.carpass.service_api.repository.ManutencaoRepository;
import com.carpass.service_api.repository.VeiculoRepository;
import com.carpass.service_api.service.interfaces.ManutencaoService;

@Service
public class ManutencaoServiceImpl implements ManutencaoService {
	
	private final ManutencaoRepository manutencaoRepository;
	private final VeiculoRepository veiculoRepository;
	
	public ManutencaoServiceImpl(ManutencaoRepository manutencaoRepository, VeiculoRepository veiculoRepository) {
		this.manutencaoRepository = manutencaoRepository;
		this.veiculoRepository = veiculoRepository;
	}

	@Override // CREATE
	public ManutencaoResponseDto salvar(ManutencaoCreateDto manutencaoDto) {
		
		Veiculo veiculo = veiculoRepository.findById(manutencaoDto.veiculo_id())
				.orElseThrow(VeiculoNaoEncontradoException::new);
		
		Manutencao registrarManutencao = new Manutencao();
		registrarManutencao.setVeiculo(veiculo);
		registrarManutencao.setPlaca(veiculo.getPlaca());
		registrarManutencao.setTipoServico(manutencaoDto.tipoServico());
		registrarManutencao.setData_manutencao(LocalDate.now());
		registrarManutencao.setKm_momento(manutencaoDto.kmMomento());
		registrarManutencao.setValor(manutencaoDto.valor());
		registrarManutencao.setDescricaoManutencao(manutencaoDto.descricaoManutencao());
		registrarManutencao.setNomeOficina(manutencaoDto.nomeOficina());
		
		manutencaoRepository.save(registrarManutencao);
		
		return new ManutencaoResponseDto(
				registrarManutencao.getId(),
				registrarManutencao.getPlaca(),
				registrarManutencao.getTipoServico(),
				registrarManutencao.getData_manutencao(),
				registrarManutencao.getKm_momento(),
				registrarManutencao.getValor(),
				registrarManutencao.getDescricaoManutencao(),
				registrarManutencao.getNomeOficina()
			);
	}

	@Override // READ BY ID
	public ManutencaoResponseDto buscarManutencaoPorId(UUID id) {	
		Manutencao manutencao = manutencaoRepository.findById(id)
				.orElseThrow(ManutencaoNaoEncontradaException::new);
		
		return new ManutencaoResponseDto(
				manutencao.getId(),
				manutencao.getPlaca(),
				manutencao.getTipoServico(),
				manutencao.getData_manutencao(),
				manutencao.getKm_momento(),
				manutencao.getValor(),
				manutencao.getDescricaoManutencao(),
				manutencao.getNomeOficina()
			);
	}

	@Override // READ ALL
	public List<ManutencaoResponseDto> buscarTodasManutencoes() {
		return manutencaoRepository.findAll()
				.stream().map(manutencao -> new ManutencaoResponseDto(
						manutencao.getId(),
						manutencao.getPlaca(),
						manutencao.getTipoServico(),
						manutencao.getData_manutencao(),
						manutencao.getKm_momento(),
						manutencao.getValor(),
						manutencao.getDescricaoManutencao(),
						manutencao.getNomeOficina())).toList();
	}
	
	@Override // UPDATE
	public ManutencaoResponseDto atualizarDadosManutencao(ManutencaoUpdateDto manutencaoDto) {
		
		Manutencao manutencao = manutencaoRepository.findById(manutencaoDto.id())
				.orElseThrow(ManutencaoNaoEncontradaException::new);
		
		Veiculo veiculo = veiculoRepository.findById(manutencaoDto.veiculo_id())
				.orElseThrow(VeiculoNaoEncontradoException::new);
		
		LocalDate dataAtual = LocalDate.now();
		
		if(manutencaoDto.dataManutencao().isAfter(dataAtual)) {
			throw new RegraDeNegocioException("Data não existente ");
		}

		manutencao.getVeiculo().setId(manutencaoDto.veiculo_id());
		manutencao.setTipoServico(manutencaoDto.tipoServico());
		manutencao.setData_manutencao(manutencaoDto.dataManutencao());
		manutencao.setKm_momento(manutencaoDto.kmMomento());
		manutencao.setValor(manutencaoDto.valor());
		manutencao.setDescricaoManutencao(manutencaoDto.descricaoManutencao());
		manutencao.setNomeOficina(manutencaoDto.nomeOficina());
		
		manutencaoRepository.save(manutencao);
		
		return new ManutencaoResponseDto(
				manutencao.getId(),
				veiculo.getPlaca(),
				manutencao.getTipoServico(),
				manutencao.getData_manutencao(),
				manutencao.getKm_momento(),
				manutencao.getValor(), 
				manutencao.getDescricaoManutencao(),
				manutencao.getNomeOficina()
			);
	}

	@Override // DELETE
	public void deletarManutencao(UUID id) {
		buscarManutencaoPorId(id);
		manutencaoRepository.deleteById(id);
	}

}
