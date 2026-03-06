package com.carpass.service_api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import com.carpass.service_api.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
	
	boolean existsByEmail(String email);
	boolean existsByNumero(String numero);
	UserDetails findByEmail(String email);
	@Query("SELECT u FROM Usuario u WHERE u.email = :email")
	Usuario buscarUsuarioPorEmail(@Param("email") String email);
	
}
