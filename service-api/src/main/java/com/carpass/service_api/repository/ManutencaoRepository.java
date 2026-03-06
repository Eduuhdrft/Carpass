package com.carpass.service_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.models.Manutencao;

public interface ManutencaoRepository extends JpaRepository<Manutencao, UUID> {

}
