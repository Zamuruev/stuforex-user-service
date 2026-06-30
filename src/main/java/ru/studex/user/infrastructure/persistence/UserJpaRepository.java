package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
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

    Optional<UserEntity> findByRefCode(String refCode);

    @Query("SELECT u FROM UserEntity u WHERE u.role = 'EXECUTOR' ORDER BY u.xpPoints DESC LIMIT :limit")
    List<UserEntity> findTopByXp(@Param("limit") int limit);


    /**
     * Топ по выполненным заданиям.
     * Если task-service — та же БД (схема tasks доступна), используй этот запрос.
     * Если разные БД — удали метод и реализуй через HTTP в LeaderboardService.
     */
    @Query(value = """
        SELECT
            u.id                                        AS userId,
            u.username                                  AS username,
            u.full_name                                 AS fullName,
            u.university                                AS university,
            u.xp_points                                 AS xpPoints,
            u.level                                     AS level,
            u.anti_plagiat_verified                     AS antiPlagiatVerified,
            COUNT(t.id)                                 AS completedTasks
        FROM users u
        LEFT JOIN tasks t ON t.executor_id = u.id AND t.status = 'DONE'
        WHERE u.role = 'EXECUTOR'
        GROUP BY u.id, u.username, u.full_name, u.university, u.xp_points, u.level, u.anti_plagiat_verified
        ORDER BY completedTasks DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Map<String, Object>> findTopByCompletedTasks(@Param("limit") int limit);

    /** Статистика по университетам */
    @Query(value = """
        SELECT
            university,
            COUNT(*)       AS member_count,
            SUM(xp_points) AS total_xp,
            0              AS total_tasks,
            0.0            AS avg_rating
        FROM users
        WHERE university IS NOT NULL
          AND role = 'EXECUTOR'
        GROUP BY university
        ORDER BY total_xp DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Map<String, Object>> findUniversityStats(@Param("limit") int limit);

    long countByRole(String role);

    long countByCreatedAtAfter(java.time.LocalDateTime date);

    @Query(value = """
        SELECT
            u.id           AS userId,
            u.username     AS username,
            u.full_name    AS fullName,
            u.anti_plagiat_requested_at AS requestedAt,
            CASE
                WHEN u.anti_plagiat_verified = true  THEN 'APPROVED'
                WHEN u.anti_plagiat_verified = false THEN 'REJECTED'
                ELSE 'PENDING'
            END AS status
        FROM users u
        WHERE u.anti_plagiat_requested_at IS NOT NULL
        ORDER BY u.anti_plagiat_requested_at DESC
        LIMIT 100
    """, nativeQuery = true)
    List<Map<String, Object>> findAntiPlagiatRequests();

    @Query(value = """
        SELECT u.* FROM users u
        WHERE (:isActive IS NULL OR u.is_active = :isActive)
          AND u.is_deleted = false
          AND (
              :q = ''
              OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.xp_points DESC
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<UserEntity> findPeople(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            @Param("size") int size,
            @Param("offset") long offset);

    @Query(value = """
        SELECT COUNT(*) FROM users u
        WHERE (:isActive IS NULL OR u.is_active = :isActive)
          AND u.is_deleted = false
          AND (
              :q = ''
              OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """, nativeQuery = true)
    long countPeople(@Param("q") String q, @Param("isActive") Boolean isActive);

}
