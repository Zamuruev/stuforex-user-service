package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_id",  columnList = "user_id"),
                @Index(name = "idx_notif_unread",   columnList = "user_id, is_read"),
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /** Ссылка для перехода при клике, напр. /marketplace/{taskId} */
    @Column(length = 500)
    private String link;

    /** JSON с доп. данными: taskId, taskTitle, fromUsername, amount */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
