package com.carpass.service_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.models.CodigoValidacao;

public interface CodigoValidacaoRepository extends JpaRepository<CodigoValidacao, UUID> {

	boolean existsByCodigo(Integer codigo);
	CodigoValidacao findByUsuario_idAndCodigo(UUID usuarioId, Integer codigo);
}
