package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioCaseRepository extends JpaRepository<PortfolioCaseEntity, UUID> {

    List<PortfolioCaseEntity> findByExecutorIdOrderByCreatedAtDesc(UUID executorId);

    @Query("""
        SELECT p FROM PortfolioCaseEntity p
        WHERE p.executorId = :executorId
          AND p.visibility IN :visibilities
        ORDER BY p.createdAt DESC
        """)
    List<PortfolioCaseEntity> findByExecutorIdAndVisibilityIn(
            @Param("executorId") UUID executorId,
            @Param("visibilities") List<String> visibilities
    );

    Optional<PortfolioCaseEntity> findByTaskId(UUID taskId);

    boolean existsByTaskId(UUID taskId);

    @Modifying
    @Query("UPDATE PortfolioCaseEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    long countByExecutorId(UUID executorId);
}
