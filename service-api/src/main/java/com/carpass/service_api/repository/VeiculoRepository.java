package com.carpass.service_api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.models.Veiculo;

public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {
	
	boolean existsByPlaca(String placa);
	List<Veiculo> findByUsuario_id(UUID id);
	
}
