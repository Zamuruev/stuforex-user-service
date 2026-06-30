package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.ProfileDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.ReferralEntity;
import ru.studex.user.infrastructure.persistence.ReferralRepository;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final JdbcTemplate jdbc;
    private final UserJpaRepository userRepo;
    private final ReferralRepository referralRepo;

    // ── АКТИВНОСТЬ ────────────────────────────────────────────

    /**
     * Возвращает карту дата→количество действий за последние 365 дней.
     * Действия: создание задания, отклик, завершение задания,
     * оставленный отзыв, оплата подписки, вход в систему (xp DAILY_LOGIN).
     * Данные берём из task-service БД (общая PostgreSQL) + user БД.
     */
    public Map<String, Integer> getActivity(String userId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(364);

        // Задания созданные пользователем
        addActivity(result, """
            SELECT DATE(created_at) AS d, COUNT(*) AS cnt
            FROM tasks
            WHERE customer_id = ?::uuid
              AND created_at >= ?
            GROUP BY d
            """, userId, from);

        // Задания завершённые (как исполнитель)
        addActivity(result, """
            SELECT DATE(COALESCE(
                (SELECT completed_at FROM escrow_accounts e WHERE e.task_id = t.id LIMIT 1),
                t.updated_at)) AS d,
                COUNT(*) AS cnt
            FROM tasks t
            WHERE t.executor_id = ?::uuid AND t.status = 'DONE'
              AND t.updated_at >= ?
            GROUP BY d
            """, userId, from);

        // Отклики оставленные исполнителем
        addActivity(result, """
            SELECT DATE(created_at) AS d, COUNT(*) AS cnt
            FROM proposals
            WHERE executor_id = ?::uuid
              AND created_at >= ?
            GROUP BY d
            """, userId, from);

        // Отзывы оставленные пользователем
        addActivity(result, """
            SELECT DATE(created_at) AS d, COUNT(*) AS cnt
            FROM reviews
            WHERE reviewer_id = ?::uuid
              AND created_at >= ?
            GROUP BY d
            """, userId, from);

        // Оплаты подписок
        addActivity(result, """
            SELECT DATE(started_at) AS d, COUNT(*) AS cnt
            FROM user_subscriptions
            WHERE user_id = ?::uuid
              AND started_at >= ?
            GROUP BY d
            """, userId, from);

        return result;
    }

    private void addActivity(Map<String, Integer> result, String sql, Object... params) {
        try {
            jdbc.query(sql, rs -> {
                String date = rs.getDate("d").toLocalDate().toString();
                int cnt = rs.getInt("cnt");
                result.merge(date, cnt, Integer::sum);
            }, params);
        } catch (Exception e) {
            // Таблица может быть недоступна (другой сервис) — игнорируем
            log.debug("Activity query failed (ok if table in another service): {}", e.getMessage());
        }
    }

    // ── ДОСТИЖЕНИЯ ────────────────────────────────────────────

    public List<ProfileDtos.AchievementDto> getAchievements(String userId) {
        UUID uid = UUID.fromString(userId);
        UserEntity user = userRepo.findById(uid)
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        boolean isExecutor = "EXECUTOR".equals(user.getRole());

        // Собираем статистику одним запросом
        long completedAsExecutor = safeCount(
                "SELECT COUNT(*) FROM tasks WHERE executor_id = ?::uuid AND status = 'DONE'", userId);
        long completedAsCustomer = safeCount(
                "SELECT COUNT(*) FROM tasks WHERE customer_id = ?::uuid AND status = 'DONE'", userId);
        long proposalsSent = safeCount(
                "SELECT COUNT(*) FROM proposals WHERE executor_id = ?::uuid", userId);
        long proposalsAccepted = safeCount(
                "SELECT COUNT(*) FROM proposals WHERE executor_id = ?::uuid AND status = 'ACCEPTED'", userId);
        double avgRating = safeDouble(
                "SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE reviewee_id = ?::uuid", userId);
        long reviewCount = safeCount(
                "SELECT COUNT(*) FROM reviews WHERE reviewee_id = ?::uuid", userId);
        long fiveStarCount = safeCount(
                "SELECT COUNT(*) FROM reviews WHERE reviewee_id = ?::uuid AND rating = 5", userId);
        long disputeCount = safeCount(
                "SELECT COUNT(*) FROM tasks WHERE executor_id = ?::uuid AND status = 'DISPUTE'", userId);
        long tasksCreated = safeCount(
                "SELECT COUNT(*) FROM tasks WHERE customer_id = ?::uuid", userId);

        int xp = user.getXpPoints() != null ? user.getXpPoints() : 0;
        boolean verified = Boolean.TRUE.equals(user.getIsVerifiedSpecialist());

        List<ProfileDtos.AchievementDto> list = new ArrayList<>();

        // ── Исполнитель ──────────────────────────────────────
        if (isExecutor) {
            list.add(ach("first_deal",   "Первая сделка",     "Выполнил первое задание",          "trophy",  "amber",   completedAsExecutor >= 1));
            list.add(ach("on_fire",      "На огне",           "5 заданий подряд без отказов",     "flame",   "red",     completedAsExecutor >= 5 && disputeCount == 0));
            list.add(ach("rating_5",     "Рейтинг 5.0",       "10 отзывов с оценкой 5",           "star",    "yellow",  fiveStarCount >= 10));
            list.add(ach("sniper",       "Снайпер",           "Выполнил 25 заданий",              "target",  "brand",   completedAsExecutor >= 25));
            list.add(ach("fast",         "Скоростной",        "Принято 10+ откликов",             "zap",     "purple",  proposalsAccepted >= 10));
            list.add(ach("top_exec",     "Топ исполнитель",   "Попал в топ-10 по XP",             "crown",   "amber",   xp >= 2000));
            list.add(ach("no_disputes",  "Без споров",        "20 заданий без единого спора",     "shield",  "emerald", completedAsExecutor >= 20 && disputeCount == 0));
            list.add(ach("century",      "Сотня",             "Выполнил 100 заданий",             "award",   "blue",    completedAsExecutor >= 100));
            list.add(ach("verified",     "Верифицирован",     "Прошёл верификацию специалиста",   "check",   "emerald", verified));
            list.add(ach("high_rating",  "Высокий рейтинг",   "Средний рейтинг выше 4.5",         "star",    "amber",   avgRating >= 4.5 && reviewCount >= 3));
        }

        // ── Студент ──────────────────────────────────────────
        if (!isExecutor) {
            list.add(ach("first_order",  "Первый заказ",      "Разместил первое задание",         "trophy",  "amber",   tasksCreated >= 1));
            list.add(ach("active_buyer", "Активный заказчик", "Разместил 10 заданий",             "target",  "brand",   tasksCreated >= 10));
            list.add(ach("done_5",       "Получил 5 работ",   "Принял 5 выполненных заданий",     "check",   "emerald", completedAsCustomer >= 5));
            list.add(ach("done_25",      "Опытный заказчик",  "Принял 25 выполненных заданий",    "award",   "blue",    completedAsCustomer >= 25));
            list.add(ach("reviewer",     "Рецензент",         "Оставил 10 отзывов",               "star",    "yellow",  reviewCount >= 10));
            list.add(ach("loyal",        "Лояльный",          "Подписка активна 3+ месяца",       "crown",   "amber",   xp >= 500));
        }

        // ── Общие ────────────────────────────────────────────
        list.add(ach("referral_1",  "Рекрутёр",       "Пригласил первого друга",           "users",   "brand",   referralRepo.countByInviterId(uid) >= 1));
        list.add(ach("referral_5",  "Амбассадор",     "Пригласил 5 активных друзей",       "gift",    "purple",  referralRepo.countActiveByInviterId(uid) >= 5));
        list.add(ach("profile_full","Полный профиль", "Заполнил все поля профиля",         "user",    "brand",   isProfileComplete(user)));
        list.add(ach("level_5",     "Уровень 5",      "Достиг 5-го уровня",               "zap",     "brand",   (user.getLevel() != null ? user.getLevel() : 1) >= 5));

        return list;
    }

    private ProfileDtos.AchievementDto ach(String id, String title, String desc,
                                           String icon, String color, boolean unlocked) {
        ProfileDtos.AchievementDto d = new ProfileDtos.AchievementDto();
        d.setId(id); d.setTitle(title); d.setDesc(desc);
        d.setIcon(icon); d.setColor(color); d.setUnlocked(unlocked);
        return d;
    }

    private boolean isProfileComplete(UserEntity u) {
        return u.getFullName() != null && !u.getFullName().isBlank() &&
                u.getBio()     != null && !u.getBio().isBlank();
    }

    // ── РЕФЕРАЛЬНАЯ ПРОГРАММА ─────────────────────────────────

    public ProfileDtos.ReferralStatsDto getReferralStats(String userId) {
        UUID uid = UUID.fromString(userId);
        UserEntity user = userRepo.findById(uid)
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        List<ReferralEntity> referrals = referralRepo.findByInviterIdOrderByCreatedAtDesc(uid);
        long activeCount = referrals.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();
        long totalCount  = referrals.size();

        // Вычислить pending reward
        String pendingReward = calcPendingReward(activeCount, referrals);

        // Собрать список рефералов с именами
        List<ProfileDtos.ReferralDto> list = referrals.stream().map(ref ->
                userRepo.findById(ref.getInviteeId()).map(invitee -> {
                    ProfileDtos.ReferralDto d = new ProfileDtos.ReferralDto();
                    d.setId(invitee.getId().toString());
                    d.setUsername(invitee.getUsername());
                    d.setRole(invitee.getRole());
                    d.setIsActive(Boolean.TRUE.equals(ref.getIsActive()));
                    d.setJoinedAt(ref.getCreatedAt());
                    return d;
                }).orElse(null)
        ).filter(Objects::nonNull).toList();

        ProfileDtos.ReferralStatsDto resp = new ProfileDtos.ReferralStatsDto();
        resp.setCode(user.getRefCode() != null ? user.getRefCode() : user.getId().toString().substring(0, 8));
        resp.setTotalInvited((int) totalCount);
        resp.setActiveUsers((int) activeCount);
        resp.setPendingReward(pendingReward);
        resp.setReferrals(list);
        return resp;
    }

    /**
     * Пометить реферала активным — вызывается когда новый пользователь
     * создаёт первое задание или отклик.
     */
    @Transactional
    public void markReferralActive(String inviteeId) {
        referralRepo.findByInviteeId(UUID.fromString(inviteeId)).ifPresent(ref -> {
            if (!Boolean.TRUE.equals(ref.getIsActive())) {
                ref.setIsActive(true);
                referralRepo.save(ref);
                log.info("Referral marked active: invitee={}", inviteeId);
            }
        });
    }

    private String calcPendingReward(long activeCount, List<ReferralEntity> refs) {
        // Уровни: 3-9 → 7 дней BASE, 10-29 → 1 мес BASE, 30+ → 1 мес PRO
        if (activeCount == 0) return null;
        int alreadyRewarded = refs.stream()
                .mapToInt(r -> r.getRewardDays() != null ? r.getRewardDays() : 0).sum();
        // Упрощённо: 7 дней за каждые 3 активных
        int earned = (int)(activeCount / 3) * 7;
        int pending = Math.max(0, earned - alreadyRewarded);
        if (pending <= 0) return null;
        if (pending >= 30) return "1 мес. BASE";
        return pending + " дней BASE";
    }

    // ── HELPERS ───────────────────────────────────────────────

    private long safeCount(String sql, Object... params) {
        try {
            Long v = jdbc.queryForObject(sql, Long.class, params);
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.debug("safeCount failed: {}", e.getMessage());
            return 0L;
        }
    }

    private double safeDouble(String sql, Object... params) {
        try {
            Double v = jdbc.queryForObject(sql, Double.class, params);
            return v != null ? v : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
