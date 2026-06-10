package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query(value = """
            SELECT * FROM users
            WHERE role = 'EXECUTOR' AND is_active = true
            ORDER BY xp_points DESC, level DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserEntity> findTopExecutors(int limit);

    @Query(value = "SELECT COUNT(*) FROM users WHERE is_active = true", nativeQuery = true)
    long countActiveUsers();

    @Query(value = """
        SELECT u.* FROM users u
        WHERE (CAST(:role AS TEXT) IS NULL OR u.role = :role)
          AND (CAST(:isActive AS BOOLEAN) IS NULL OR u.is_active = CAST(:isActive AS BOOLEAN))
          AND (
              CAST(:q AS TEXT) IS NULL
              OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
              OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
              OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
          )
        ORDER BY u.created_at DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<UserEntity> adminSearch(
            @org.springframework.data.repository.query.Param("q") String query,
            @org.springframework.data.repository.query.Param("role") String role,
            @org.springframework.data.repository.query.Param("size") int size,
            @org.springframework.data.repository.query.Param("offset") long offset,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive);

    @Query(value = """
            SELECT COUNT(*) FROM users u
            WHERE (CAST(:role AS TEXT) IS NULL OR u.role = :role)
              AND (CAST(:isActive AS BOOLEAN) IS NULL OR u.is_active = CAST(:isActive AS BOOLEAN))
              AND (CAST(:q AS TEXT) IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
                   OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS TEXT), '%')))
            """, nativeQuery = true)
    long adminSearchCount(
            @org.springframework.data.repository.query.Param("q") String query,
            @org.springframework.data.repository.query.Param("role") String role,
            @org.springframework.data.repository.query.Param("isActive") Boolean isActive);
}
