package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.PortfolioDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.PortfolioCaseEntity;
import ru.studex.user.infrastructure.persistence.PortfolioCaseRepository;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioCaseRepository portfolioRepo;
    private final UserJpaRepository userRepo;
    private final JdbcTemplate jdbc;

    // ── GET CASES ─────────────────────────────────────────────

    /**
     * Возвращает список кейсов исполнителя.
     * isOwner=true → все кейсы включая HIDDEN.
     * isOwner=false → только PUBLIC и LINK.
     */
    public List<PortfolioDtos.PortfolioCaseResponse> getCases(
            String executorId, boolean isOwner) {

        UUID uid = UUID.fromString(executorId);

        List<PortfolioCaseEntity> cases = isOwner
                ? portfolioRepo.findByExecutorIdOrderByCreatedAtDesc(uid)
                : portfolioRepo.findByExecutorIdAndVisibilityIn(uid, List.of("PUBLIC", "LINK"));

        return cases.stream()
                .map(c -> enrichCase(c, executorId))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Обогащает кейс данными из tasks, reviews, escrow_accounts.
     * Возвращает null если задание не найдено (было удалено).
     */
    private PortfolioDtos.PortfolioCaseResponse enrichCase(
            PortfolioCaseEntity c, String executorId) {
        try {
            // Данные из tasks + escrow + reviews одним запросом
            return jdbc.queryForObject("""
                SELECT
                    t.id              AS task_id,
                    t.title,
                    t.description,
                    t.subject,
                    t.tags,
                    t.deadline,
                    t.status          AS task_status,
                    COALESCE(e.completed_at, t.updated_at) AS completed_at,
                    COALESCE(t.agreed_price, e.amount)     AS budget,
                    r.rating,
                    r.comment         AS review,
                    cu.full_name      AS customer_name,
                    cu.username       AS customer_username,
                    cu.is_verified    AS customer_verified,
                    cat.name          AS category_name
                FROM tasks t
                LEFT JOIN escrow_accounts e
                    ON e.task_id = t.id
                LEFT JOIN reviews r
                    ON r.task_id = t.id
                   AND r.reviewee_id = ?::uuid
                LEFT JOIN users cu
                    ON cu.id = t.customer_id
                LEFT JOIN categories cat
                    ON cat.id = t.category_id
                WHERE t.id = ?::uuid
                """,
                    (rs, rowNum) -> {
                        PortfolioDtos.PortfolioCaseResponse dto = new PortfolioDtos.PortfolioCaseResponse();

                        dto.setId(c.getId().toString());
                        dto.setTaskId(rs.getString("task_id"));
                        dto.setTitle(rs.getString("title"));
                        dto.setDescription(rs.getString("description"));
                        dto.setSubject(rs.getString("subject"));
                        dto.setCategory(rs.getString("category_name"));

                        // tags[] → List<String>
                        var tagsArr = rs.getArray("tags");
                        if (tagsArr != null) {
                            dto.setSkills(Arrays.asList((String[]) tagsArr.getArray()));
                        } else {
                            dto.setSkills(List.of());
                        }

                        // Даты
                        var deadline = rs.getTimestamp("deadline");
                        var completedAt = rs.getTimestamp("completed_at");
                        if (deadline != null)     dto.setDeadline(deadline.toLocalDateTime());
                        if (completedAt != null)  dto.setCompletedAt(completedAt.toLocalDateTime());

                        // Сколько дней раньше дедлайна
                        if (dto.getDeadline() != null && dto.getCompletedAt() != null) {
                            long days = java.time.temporal.ChronoUnit.DAYS.between(
                                    dto.getCompletedAt().toLocalDate(),
                                    dto.getDeadline().toLocalDate());
                            dto.setDaysAhead((int) Math.max(0, days));
                        } else {
                            dto.setDaysAhead(0);
                        }

                        // Бюджет
                        BigDecimal budget = rs.getBigDecimal("budget");
                        dto.setBudget(budget);
                        dto.setBudgetHidden(false);

                        // Отзыв
                        int rating = rs.getInt("rating");
                        dto.setRating(rs.wasNull() ? null : rating);
                        dto.setReview(rs.getString("review"));

                        // Заказчик
                        String custName = rs.getString("customer_name");
                        String custUser = rs.getString("customer_username");
                        dto.setCustomerName(custName != null ? custName : custUser);
                        dto.setCustomerVerified(rs.getBoolean("customer_verified"));
                        dto.setCustomerAnonymous(false);

                        // Из portfolio_cases
                        dto.setVisibility(c.getVisibility());
                        dto.setAiSummary(c.getAiSummary());
                        dto.setProofHash(c.getProofHash());
                        dto.setViewCount(c.getViewCount());
                        dto.setCreatedAt(c.getCreatedAt());

                        // Вычисляемые поля
                        String taskStatus = rs.getString("task_status");
                        dto.setStatus(mapStatus(taskStatus));
                        dto.setVerificationLevel(calcVerificationLevel(dto));
                        dto.setComplexity(calcComplexity(budget));
                        dto.setImpact(calcImpact(dto.getComplexity()));

                        return dto;
                    },
                    executorId, c.getTaskId().toString()
            );
        } catch (Exception e) {
            log.warn("enrichCase failed for taskId={}: {}", c.getTaskId(), e.getMessage());
            return null;
        }
    }

    // ── GET STATS ─────────────────────────────────────────────

    public PortfolioDtos.PortfolioStatsResponse getStats(String executorId) {
        UUID uid = UUID.fromString(executorId);

        PortfolioDtos.PortfolioStatsResponse stats = new PortfolioDtos.PortfolioStatsResponse();

        // Всего кейсов
        int total = (int) portfolioRepo.countByExecutorId(uid);
        stats.setTotalProjects(total);

        // Верифицированные (есть в portfolio_cases + задание DONE + есть отзыв)
        Integer verified = safeInt("""
            SELECT COUNT(*)
            FROM portfolio_cases pc
            JOIN tasks t ON t.id = pc.task_id
            JOIN reviews r ON r.task_id = t.id AND r.reviewee_id = ?::uuid
            WHERE pc.executor_id = ?::uuid AND t.status = 'DONE'
            """, executorId, executorId);
        stats.setVerifiedProjects(verified);

        // Сумма заработанного
        BigDecimal totalEarned = safeDecimal("""
            SELECT COALESCE(SUM(COALESCE(t.agreed_price, e.amount)), 0)
            FROM portfolio_cases pc
            JOIN tasks t ON t.id = pc.task_id
            LEFT JOIN escrow_accounts e ON e.task_id = t.id
            WHERE pc.executor_id = ?::uuid AND t.status = 'DONE'
            """, executorId);
        stats.setTotalEarned(totalEarned);

        // Средняя стоимость
        BigDecimal avgOrder = total > 0
                ? totalEarned.divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.setAvgOrderValue(avgOrder);

        // Средний рейтинг
        Double avgRating = safeDouble("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM reviews r
            WHERE r.reviewee_id = ?::uuid
            """, executorId);
        stats.setAvgRating(round1(avgRating));

        // Успешность (DONE / всего заданий исполнителя)
        Integer allTasks = safeInt("""
            SELECT COUNT(*) FROM tasks WHERE executor_id = ?::uuid
            """, executorId);
        Integer doneTasks = safeInt("""
            SELECT COUNT(*) FROM tasks
            WHERE executor_id = ?::uuid AND status = 'DONE'
            """, executorId);
        double successRate = allTasks > 0 ? (doneTasks * 100.0 / allTasks) : 0;
        stats.setSuccessRate(round1(successRate));

        // Повторные клиенты
        Integer repeatCustomers = safeInt("""
            SELECT COUNT(DISTINCT customer_id)
            FROM tasks
            WHERE executor_id = ?::uuid AND status = 'DONE'
              AND customer_id IN (
                  SELECT customer_id FROM tasks
                  WHERE executor_id = ?::uuid AND status = 'DONE'
                  GROUP BY customer_id HAVING COUNT(*) > 1
              )
            """, executorId, executorId);
        Integer totalCustomers = safeInt("""
            SELECT COUNT(DISTINCT customer_id) FROM tasks
            WHERE executor_id = ?::uuid AND status = 'DONE'
            """, executorId);
        double repeatRate = totalCustomers > 0 ? (repeatCustomers * 100.0 / totalCustomers) : 0;
        stats.setRepeatClientRate(round1(repeatRate));

        // Trust Score и уровень доверия
        stats.setReputationScore(calcReputationScore(stats));
        stats.setTrustLevel(calcTrustLevel(stats));

        // Топ навыки из tags[] заданий
        stats.setTopSkills(calcTopSkills(executorId));

        // Помесячный рост (последние 12 месяцев)
        stats.setMonthlyGrowth(calcMonthlyGrowth(executorId));

        // Граф связей
        stats.setConnections(calcConnections(executorId));

        return stats;
    }

    // ── CREATE CASE (автоматически при DONE) ─────────────────

    /**
     * Вызывается из TaskService.completeTask() через HTTP.
     * Создаёт кейс только если задание действительно DONE и исполнитель есть.
     */
    @Transactional
    public void createCaseFromTask(String taskId) {
        // Не создавать дважды
        UUID tid = UUID.fromString(taskId);
        if (portfolioRepo.existsByTaskId(tid)) {
            log.debug("Portfolio case already exists for taskId={}", taskId);
            return;
        }

        // Получаем данные задания
        try {
            Map<String, Object> task = jdbc.queryForMap("""
                SELECT t.executor_id, t.agreed_price, t.updated_at,
                       COALESCE(e.completed_at, t.updated_at) AS completed_at
                FROM tasks t
                LEFT JOIN escrow_accounts e ON e.task_id = t.id
                WHERE t.id = ?::uuid AND t.executor_id IS NOT NULL
                """, taskId);

            UUID executorId = (UUID) task.get("executor_id");
            Object completedAt = task.get("completed_at");
            Object price = task.get("agreed_price");

            String proofHash = generateProofHash(taskId, executorId.toString(),
                    completedAt != null ? completedAt.toString() : "",
                    price != null ? price.toString() : "0");

            PortfolioCaseEntity caseEntity = PortfolioCaseEntity.builder()
                    .executorId(executorId)
                    .taskId(tid)
                    .visibility("PUBLIC")
                    .proofHash(proofHash)
                    .viewCount(0)
                    .build();

            portfolioRepo.save(caseEntity);
            log.info("Portfolio case created for taskId={}, executorId={}", taskId, executorId);

        } catch (Exception e) {
            log.warn("Failed to create portfolio case for taskId={}: {}", taskId, e.getMessage());
        }
    }

    // ── UPDATE CASE ───────────────────────────────────────────

    @Transactional
    public PortfolioDtos.PortfolioCaseResponse updateCase(
            String caseId, String requesterId, PortfolioDtos.UpdateCaseRequest req) {

        PortfolioCaseEntity c = portfolioRepo.findById(UUID.fromString(caseId))
                .orElseThrow(() -> new ApiException("Кейс не найден", 404));

        if (!c.getExecutorId().toString().equals(requesterId))
            throw new ApiException("Доступ запрещён", 403);

        if (req.getVisibility() != null) {
            validateVisibility(req.getVisibility());
            c.setVisibility(req.getVisibility());
        }
        if (req.getAiSummary() != null) {
            c.setAiSummary(req.getAiSummary());
        }

        portfolioRepo.save(c);
        return enrichCase(c, requesterId);
    }

    // ── INCREMENT VIEW ────────────────────────────────────────

    @Transactional
    public void incrementView(String caseId) {
        try {
            portfolioRepo.incrementViewCount(UUID.fromString(caseId));
        } catch (Exception e) {
            log.debug("incrementView failed: {}", e.getMessage());
        }
    }

    // ── HELPERS / CALCULATORS ─────────────────────────────────

    private List<PortfolioDtos.SkillStat> calcTopSkills(String executorId) {
        try {
            // Разворачиваем tags[] и считаем частоту
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT skill, COUNT(*) AS cnt
                FROM (
                    SELECT unnest(tags) AS skill
                    FROM tasks
                    WHERE executor_id = ?::uuid AND status = 'DONE'
                ) s
                WHERE skill IS NOT NULL AND skill != ''
                GROUP BY skill
                ORDER BY cnt DESC
                LIMIT 8
                """, executorId);

            if (rows.isEmpty()) return List.of();

            long max = ((Number) rows.get(0).get("cnt")).longValue();

            return rows.stream().map(row -> {
                PortfolioDtos.SkillStat s = new PortfolioDtos.SkillStat();
                s.setName((String) row.get("skill"));
                long cnt = ((Number) row.get("cnt")).longValue();
                s.setCount((int) cnt);
                s.setLevel(max > 0 ? (int) Math.ceil(cnt * 5.0 / max) : 1);
                return s;
            }).toList();
        } catch (Exception e) {
            log.debug("calcTopSkills failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PortfolioDtos.MonthStat> calcMonthlyGrowth(String executorId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT
                    TO_CHAR(DATE_TRUNC('month', COALESCE(e.completed_at, t.updated_at)), 'YYYY-MM') AS ym,
                    COUNT(*)                                  AS projects,
                    COALESCE(SUM(COALESCE(t.agreed_price, e.amount)), 0) AS earned
                FROM tasks t
                LEFT JOIN escrow_accounts e ON e.task_id = t.id
                WHERE t.executor_id = ?::uuid
                  AND t.status = 'DONE'
                  AND COALESCE(e.completed_at, t.updated_at) >= NOW() - INTERVAL '12 months'
                GROUP BY ym
                ORDER BY ym ASC
                """, executorId);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
            java.time.YearMonth now = java.time.YearMonth.now();
            java.time.YearMonth from = now.minusMonths(11);

            // Заполняем все 12 месяцев, даже пустые
            Map<String, PortfolioDtos.MonthStat> map = new LinkedHashMap<>();
            for (int i = 0; i < 12; i++) {
                java.time.YearMonth ym = from.plusMonths(i);
                String key = ym.format(fmt);
                PortfolioDtos.MonthStat ms = new PortfolioDtos.MonthStat();
                ms.setMonth(ym.getMonth()
                        .getDisplayName(java.time.format.TextStyle.SHORT, new Locale("ru")));
                ms.setProjects(0);
                ms.setEarned(BigDecimal.ZERO);
                map.put(key, ms);
            }
            rows.forEach(row -> {
                String key = (String) row.get("ym");
                if (map.containsKey(key)) {
                    map.get(key).setProjects(((Number) row.get("projects")).intValue());
                    map.get(key).setEarned((BigDecimal) row.get("earned"));
                }
            });
            return new ArrayList<>(map.values());
        } catch (Exception e) {
            log.debug("calcMonthlyGrowth failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PortfolioDtos.ConnectionStat> calcConnections(String executorId) {
        try {
            return jdbc.query("""
                SELECT
                    cu.id::text   AS id,
                    COALESCE(cu.full_name, cu.username) AS name,
                    cu.university AS role,
                    COUNT(*)      AS projects_together
                FROM tasks t
                JOIN users cu ON cu.id = t.customer_id
                WHERE t.executor_id = ?::uuid
                  AND t.status = 'DONE'
                GROUP BY cu.id, cu.full_name, cu.username, cu.university
                ORDER BY projects_together DESC
                LIMIT 6
                """,
                    (rs, i) -> {
                        PortfolioDtos.ConnectionStat s = new PortfolioDtos.ConnectionStat();
                        s.setId(rs.getString("id"));
                        s.setName(rs.getString("name"));
                        s.setRole(rs.getString("role") != null ? rs.getString("role") : "Студент");
                        s.setProjectsTogether(rs.getInt("projects_together"));
                        return s;
                    },
                    executorId
            );
        } catch (Exception e) {
            log.debug("calcConnections failed: {}", e.getMessage());
            return List.of();
        }
    }

    private int calcReputationScore(PortfolioDtos.PortfolioStatsResponse s) {
        // Формула: avgRating*16 + successRate*0.3 + min(verifiedProjects,20)*1.5 + repeatRate*0.2
        double score = s.getAvgRating() * 16
                + s.getSuccessRate() * 0.3
                + Math.min(s.getVerifiedProjects(), 20) * 1.5
                + s.getRepeatClientRate() * 0.2;
        return Math.min(100, (int) score);
    }

    private int calcTrustLevel(PortfolioDtos.PortfolioStatsResponse s) {
        int score = s.getReputationScore();
        if (score >= 90) return 5;
        if (score >= 75) return 4;
        if (score >= 55) return 3;
        if (score >= 35) return 2;
        return 1;
    }

    private String mapStatus(String taskStatus) {
        if ("DONE".equals(taskStatus))     return "verified";
        if ("DISPUTE".equals(taskStatus))  return "disputed";
        return "completed";
    }

    private String calcVerificationLevel(PortfolioDtos.PortfolioCaseResponse dto) {
        // Если есть отзыв от заказчика с рейтингом — PLATFORM
        if (dto.getRating() != null && dto.getRating() >= 1) return "PLATFORM";
        // Если задание DONE без отзыва — REVIEW
        if ("verified".equals(dto.getStatus()))              return "REVIEW";
        return "NONE";
    }

    private String calcComplexity(BigDecimal budget) {
        if (budget == null) return "low";
        double b = budget.doubleValue();
        if (b >= 10000) return "expert";
        if (b >= 5000)  return "high";
        if (b >= 2000)  return "medium";
        return "low";
    }

    private int calcImpact(String complexity) {
        return switch (complexity) {
            case "expert" -> 300;
            case "high"   -> 200;
            case "medium" -> 120;
            default       -> 60;
        };
    }

    private String generateProofHash(String taskId, String executorId,
                                     String completedAt, String price) {
        try {
            String input = taskId + "|" + executorId + "|" + completedAt + "|" + price;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "0x" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    private void validateVisibility(String v) {
        if (!List.of("PUBLIC","LINK","CLIENTS","HIDDEN").contains(v))
            throw new ApiException("Некорректное значение visibility: " + v, 400);
    }

    // ── SQL helpers ───────────────────────────────────────────

    private Integer safeInt(String sql, Object... params) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, params);
            return v != null ? v : 0;
        } catch (Exception e) { return 0; }
    }

    private BigDecimal safeDecimal(String sql, Object... params) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, params);
            return v != null ? v : BigDecimal.ZERO;
        } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private Double safeDouble(String sql, Object... params) {
        try {
            Double v = jdbc.queryForObject(sql, Double.class, params);
            return v != null ? v : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
