package ru.studex.user.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PortfolioDtos {

    // ── Кейс портфолио (полный) ───────────────────────────────

    @Data
    public static class PortfolioCaseResponse {
        private String id;
        private String taskId;

        // Из tasks
        private String title;
        private String description;
        private String subject;
        private String category;
        private List<String> skills;        // tags[] из tasks
        private LocalDateTime deadline;
        private LocalDateTime completedAt;  // escrow.completed_at или task.updated_at
        private Integer daysAhead;          // deadline - completedAt в днях (>0 = раньше)
        private Integer pages;              // null (нет в БД, добавить позже если нужно)

        // Из escrow_accounts
        private BigDecimal budget;          // agreed_price
        private boolean budgetHidden;

        // Из reviews
        private Integer rating;
        private String review;              // comment
        private String customerName;        // reviewer.full_name или username
        private boolean customerVerified;   // is_verified
        private boolean customerAnonymous;  // если reviewer не захотел показывать имя

        // Из portfolio_cases
        private String visibility;          // PUBLIC | LINK | CLIENTS | HIDDEN
        private String aiSummary;
        private String proofHash;
        private Integer viewCount;

        // Вычисляемые
        private String status;              // verified | completed | disputed
        private String verificationLevel;  // PLATFORM | REVIEW | NONE
        private String complexity;          // low | medium | high | expert (по budget)
        private Integer impact;             // XP бонус (по complexity)
        private LocalDateTime createdAt;
    }

    // ── Статистика портфолио ──────────────────────────────────

    @Data
    public static class PortfolioStatsResponse {
        private int totalProjects;
        private BigDecimal totalEarned;
        private double avgRating;
        private double successRate;         // % DONE от всех задан исполнителя
        private double repeatClientRate;    // % заказчиков с 2+ заказами
        private BigDecimal avgOrderValue;
        private int verifiedProjects;       // с verificationLevel=PLATFORM
        private int reputationScore;        // 0-100
        private int trustLevel;             // 1-5

        private List<SkillStat> topSkills;
        private List<MonthStat> monthlyGrowth;
        private List<ConnectionStat> connections;
    }

    @Data
    public static class SkillStat {
        private String name;
        private int count;
        private int level;                  // 1-5 (count/max * 5)
    }

    @Data
    public static class MonthStat {
        private String month;               // "янв", "фев", ...
        private int projects;
        private BigDecimal earned;
    }

    @Data
    public static class ConnectionStat {
        private String id;
        private String name;
        private String role;                // customer | collaborator
        private int projectsTogether;
    }

    // ── Обновление кейса ─────────────────────────────────────

    @Data
    public static class UpdateCaseRequest {
        private String visibility;
        private String aiSummary;
    }
}
