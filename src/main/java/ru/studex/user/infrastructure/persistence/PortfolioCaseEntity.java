package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolio_cases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "executor_id", nullable = false)
    private UUID executorId;

    @Column(name = "task_id", nullable = false, unique = true)
    private UUID taskId;

    /** PUBLIC | LINK | CLIENTS | HIDDEN */
    @Column(nullable = false)
    @Builder.Default
    private String visibility = "PUBLIC";

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /** SHA-256(taskId + executorId + completedAt + agreedPrice) */
    @Column(name = "proof_hash", nullable = false, length = 128)
    private String proofHash;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
