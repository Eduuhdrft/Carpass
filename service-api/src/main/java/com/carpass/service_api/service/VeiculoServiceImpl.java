package com.carpass.service_api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.carpass.service_api.dto.veiculoDto.VeiculoCreateDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoResponseDto;
import com.carpass.service_api.dto.veiculoDto.VeiculoUpdateDto;
import com.carpass.service_api.exceptions.RegraDeNegocioException;
import com.carpass.service_api.exceptions.UsuarioNaoEncontradoException;
import com.carpass.service_api.exceptions.VeiculoNaoEncontradoException;
import com.carpass.service_api.models.Usuario;
import com.carpass.service_api.models.Veiculo;
import com.carpass.service_api.repository.UsuarioRepository;
import com.carpass.service_api.repository.VeiculoRepository;
import com.carpass.service_api.service.interfaces.VeiculoService;
import com.carpass.service_api.validation.veiculo.VeiculoValidator;

@Service
public class VeiculoServiceImpl implements VeiculoService {

	private final VeiculoRepository veiculoRepository;
	private final UsuarioRepository usuarioRepository;
	private final VeiculoValidator veiculoValidator;

	public VeiculoServiceImpl(VeiculoRepository veiculoRepository, UsuarioRepository usuarioRepostory,
			VeiculoValidator veiculoValidator) {
		this.veiculoRepository = veiculoRepository;
		this.usuarioRepository = usuarioRepostory;
		this.veiculoValidator = veiculoValidator;
	}

	@Override // CREATE
	public VeiculoResponseDto salvar(VeiculoCreateDto veiculoDto) {

		Usuario usuario = usuarioRepository.findById(veiculoDto.usuario_id())
				.orElseThrow(UsuarioNaoEncontradoException::new);

		if (veiculoRepository.existsByPlaca(veiculoDto.placa()))
			throw new RegraDeNegocioException("Um veículo com esta placa já esta cadastrado");

		veiculoValidator.validarAnoDeFabricacao(veiculoDto.anoDeFabricacao());

		Veiculo criandoVeiculo = new Veiculo();

		criandoVeiculo.setPlaca(veiculoDto.placa());
		criandoVeiculo.setMarca(veiculoDto.marca());
		criandoVeiculo.setModelo(veiculoDto.modelo());
		criandoVeiculo.setAnoDeFabricacao(veiculoDto.anoDeFabricacao());
		criandoVeiculo.setCor(veiculoDto.cor());
		criandoVeiculo.setUsuario(usuario);

		veiculoRepository.save(criandoVeiculo);

		return new VeiculoResponseDto(criandoVeiculo.getId(), criandoVeiculo.getPlaca(), criandoVeiculo.getMarca(),
				criandoVeiculo.getModelo(), criandoVeiculo.getAnoDeFabricacao(), criandoVeiculo.getCor(),
				criandoVeiculo.getUsuario().getId());
	}

	@Override // READ BY ID
	public VeiculoResponseDto buscarVeiculoPorId(UUID id) {

		Veiculo veiculo = veiculoRepository.findById(id).orElseThrow(VeiculoNaoEncontradoException::new);

		return new VeiculoResponseDto(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(), veiculo.getModelo(),
				veiculo.getAnoDeFabricacao(), veiculo.getCor(), veiculo.getUsuario().getId());
	}

	@Override // READ ALL
	public List<VeiculoResponseDto> buscarTodosVeiculos() {
		return veiculoRepository.findAll() // Retorna uma lista
				.stream() // Percorre a lista
				.map( // Converte cada entidade para dto
						veiculo -> new VeiculoResponseDto(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(),
								veiculo.getModelo(), veiculo.getAnoDeFabricacao(), veiculo.getCor(),
								veiculo.getUsuario().getId()))
				.toList();
	}

	@Override // PUT
	public VeiculoResponseDto atualizarDadosVeiculo(VeiculoUpdateDto veiculoDto) {

		Veiculo veiculo = veiculoRepository.findById(veiculoDto.id()).orElseThrow(VeiculoNaoEncontradoException::new);

		Usuario usuario = usuarioRepository.findById(veiculoDto.usuario_id())
				.orElseThrow(UsuarioNaoEncontradoException::new);

		if (!veiculo.getPlaca().equals(veiculoDto.placa()) && veiculoRepository.existsByPlaca(veiculoDto.placa())) {
			throw new RegraDeNegocioException("Esta placa já está cadastrada");
		}

		veiculoValidator.validarAnoDeFabricacao(veiculoDto.anoDeFabricacao());

		veiculo.setPlaca(veiculoDto.placa());
		veiculo.setMarca(veiculoDto.marca());
		veiculo.setModelo(veiculoDto.modelo());
		veiculo.setAnoDeFabricacao(veiculoDto.anoDeFabricacao());
		veiculo.setCor(veiculoDto.cor());
		veiculo.setUsuario(usuario);

		veiculoRepository.save(veiculo);

		return new VeiculoResponseDto(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(), veiculo.getModelo(),
				veiculo.getAnoDeFabricacao(), veiculo.getCor(), veiculo.getUsuario().getId());

	}

	@Override // DELETE
	public void deletarVeiculo(UUID id) {
		buscarVeiculoPorId(id);
		veiculoRepository.deleteById(id);
	}

	@Override // READ
	public List<VeiculoResponseDto> buscarPorUsuarioId(UUID id) {
		return veiculoRepository.findByUsuario_id(id).stream()
				.map(veiculo -> new VeiculoResponseDto(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(),
						veiculo.getModelo(), veiculo.getAnoDeFabricacao(), veiculo.getCor(),
						veiculo.getUsuario().getId()))
				.toList();
	}

}