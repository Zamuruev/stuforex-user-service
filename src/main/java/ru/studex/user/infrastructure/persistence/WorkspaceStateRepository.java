package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceStateRepository extends JpaRepository<WorkspaceStateEntity, UUID> {
    Optional<WorkspaceStateEntity> findByUserId(UUID userId);
}
