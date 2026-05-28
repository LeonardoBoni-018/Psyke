package com.br.psyke.psyke.repository;

import com.br.psyke.psyke.model.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, UUID> {
    Optional<ConfirmationToken> findByToken(UUID token);
}
