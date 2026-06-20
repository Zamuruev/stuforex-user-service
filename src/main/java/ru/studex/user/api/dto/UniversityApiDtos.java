package ru.studex.user.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class UniversityApiDtos {

    // ── Запрос на доступ (публичная форма) ───────────────────

    @Data
    public static class AccessRequest {
        private String university;   // название вуза
        private String email;        // корпоративный email
        private String position;     // должность (необязательно)
        private String comment;      // комментарий (необязательно)
    }

    // ── Ответ на запрос доступа ───────────────────────────────

    @Data
    public static class AccessResponse {
        private String message;
        private String status;       // PENDING
    }

    // ── Основная статистика ───────────────────────────────────

    @Data
    public static class UniversityStatsResponse {
        private String university;
        private String period;
        private int totalRequests;
        private String totalRequestsTrend;
        private int activeSubjects;
        private BigDecimal avgBudget;
        private List<SubjectStat> topSubjects;
        private Map<String, Integer> courseYearBreakdown;
        private List<WeekStat> weeklyActivity;
        private PeakInfo peakWeek;
    }

    @Data
    public static class SubjectStat {
        private String subject;
        private int count;
        private String trend;        // "+24%" или "-3%"
        private boolean trendUp;
        private BigDecimal avgBudget;
        private int pct;             // % от максимума для фронтенда
    }

    @Data
    public static class WeekStat {
        private String week;         // "Янв", "Фев", ...
        private String label;        // "Сессия" или пусто
        private int requests;
    }

    @Data
    public static class PeakInfo {
        private String week;
        private int requests;
        private String label;
    }

    // ── Топ предметов отдельным эндпоинтом ───────────────────

    @Data
    public static class TopSubjectsResponse {
        private String university;
        private String period;
        private List<SubjectStat> subjects;
    }

    // ── Пики активности ───────────────────────────────────────

    @Data
    public static class PeakPeriodsResponse {
        private String university;
        private String period;
        private List<WeekStat> weekly;
        private PeakInfo peak;
    }

    // ── Для Admin: список заявок ──────────────────────────────

    @Data
    public static class ApiKeyRecord {
        private String id;
        private String universityName;
        private String contactEmail;
        private String position;
        private String comment;
        private String status;
        private String createdAt;
        private String approvedAt;
        private String lastUsedAt;
        private String apiKey;       // только для ACTIVE записей
    }
}
