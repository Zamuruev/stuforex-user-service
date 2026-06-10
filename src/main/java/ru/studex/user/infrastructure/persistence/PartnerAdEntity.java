package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_ads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartnerAdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String company;

    @Column(length = 100)
    private String tagline;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String cta = "Подробнее →";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "accent_color", nullable = false, length = 200)
    @Builder.Default
    private String accentColor = "bg-violet-600 hover:bg-violet-700";

    @Column(name = "bg_color", nullable = false, length = 200)
    @Builder.Default
    private String bgColor = "bg-violet-50 dark:bg-violet-900/10";

    @Column(name = "border_color", nullable = false, length = 200)
    @Builder.Default
    private String borderColor = "border-violet-200 dark:border-violet-800/50";

    @Column(length = 10)
    private String logo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
