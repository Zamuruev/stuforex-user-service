package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioFileRepository extends JpaRepository<PortfolioFileEntity, UUID> {
    List<PortfolioFileEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
