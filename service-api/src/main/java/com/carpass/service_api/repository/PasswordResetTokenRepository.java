package com.carpass.service_api.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carpass.service_api.domain.model.PasswordResetToken;

/**
 * Tokens de redefinicao de senha.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	/** Invalida os tokens ainda abertos de um usuario ao emitir um novo. */
	@Modifying
	@Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
	int invalidateOpenTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
