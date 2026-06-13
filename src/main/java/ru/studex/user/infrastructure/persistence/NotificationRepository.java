package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /** Все уведомления пользователя, новые первые, максимум 100 */
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Количество непрочитанных */
    long countByUserIdAndIsRead(UUID userId, boolean isRead);

    /** Пометить одно как прочитанное */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    void markRead(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Пометить все как прочитанные */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllRead(@Param("userId") UUID userId);

    /** Удалить старые уведомления (чистка раз в день) */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :before")
    void deleteOlderThan(@Param("before") LocalDateTime before);
}
