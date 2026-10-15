package com.carpass.service_api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carpass.service_api.domain.model.User;

/**
 * Identidades da plataforma.
 *
 * <p>
 * Nao ha mais consulta de usuario por oficina aqui: a pessoa nao pertence a uma
 * oficina, o <em>vinculo</em> dela e que pertence. Quem responde "esta pessoa
 * faz parte desta oficina?" e o {@link WorkshopMemberRepository}.
 * </p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
