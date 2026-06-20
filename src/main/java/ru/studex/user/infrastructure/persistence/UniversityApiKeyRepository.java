package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UniversityApiKeyRepository extends JpaRepository<UniversityApiKeyEntity, UUID> {

    Optional<UniversityApiKeyEntity> findByApiKeyAndStatus(String apiKey, String status);

    Optional<UniversityApiKeyEntity> findByApiKey(String apiKey);

    List<UniversityApiKeyEntity> findAllByOrderByCreatedAtDesc();

    List<UniversityApiKeyEntity> findByStatus(String status);

    boolean existsByContactEmailAndStatusNot(String email, String status);

    @Modifying
    @Query("UPDATE UniversityApiKeyEntity k SET k.lastUsedAt = :now WHERE k.apiKey = :key")
    void touchLastUsed(@Param("key") String key, @Param("now") LocalDateTime now);
}
