package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "referrals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;
    @Column(name = "invitee_id", nullable = false)
    private UUID inviteeId;
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;
    @Column(name = "reward_days")
    private Integer rewardDays;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
