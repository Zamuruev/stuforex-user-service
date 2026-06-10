package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private String role = "STUDENT";

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    private String bio;
    private String university;

    @Column(name = "xp_points")
    @Builder.Default
    private Integer xpPoints = 0;

    @Builder.Default
    private Integer level = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String phone;
    private String telegram;
    private String specialization;

    @Column(name = "last_daily_login")
    private LocalDate lastDailyLogin;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skillsCsv;

    // ── Подписка ──────────────────────────────────────────────

    /** LITE | BASE | PRO */
    @Column(name = "subscription_plan")
    @Builder.Default
    private String subscriptionPlan = "LITE";

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    /** Сколько откликов исполнитель оставил в текущем месяце */
    @Column(name = "proposals_this_month")
    @Builder.Default
    private Integer proposalsThisMonth = 0;

    /** Когда сбрасывать счётчик */
    @Column(name = "proposals_reset_at")
    private LocalDateTime proposalsResetAt;

    @Column(name = "is_verified_specialist")
    @Builder.Default
    private Boolean isVerifiedSpecialist = false;

    /** Реферальный код — первые 8 символов UUID, устанавливается при регистрации */
    @Column(name = "ref_code", unique = true)
    private String refCode;

    // ── Transient helpers ─────────────────────────────────────

    @Transient
    public String[] getSkills() {
        if (skillsCsv == null || skillsCsv.isBlank()) return new String[0];
        return skillsCsv.split(",");
    }

    @Transient
    public void setSkillsArray(String[] skills) {
        this.skillsCsv = (skills == null || skills.length == 0) ? null : String.join(",", skills);
    }

    /** Лимит откликов по текущему плану. null = безлимит (PRO). */
    @Transient
    public Integer getProposalsLimit() {
        if (subscriptionPlan == null) return 5;
        return switch (subscriptionPlan) {
            case "BASE" -> 30;
            case "PRO"  -> null;   // безлимит
            default     -> 5;      // LITE
        };
    }

    /** Лимит публикаций заданий для студентов по текущему плану. null = безлимит. */
    @Transient
    public Integer getTasksLimit() {
        if (subscriptionPlan == null) return 3;
        return switch (subscriptionPlan) {
            case "BASE" -> 8;
            case "PRO"  -> null;  // безлимит
            default     -> 3;     // LITE
        };
    }

    /** Подписка ещё активна? */
    @Transient
    public boolean isSubscriptionActive() {
        if ("LITE".equals(subscriptionPlan)) return true;
        return subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }
}
