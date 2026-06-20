package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "university_api_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UniversityApiKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "university_id")
    private UUID universityId;

    @Column(name = "university_name", nullable = false)
    private String universityName;

    /** uk_live_<32 random hex chars> */
    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "position")
    private String position;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** PENDING | ACTIVE | REVOKED */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
