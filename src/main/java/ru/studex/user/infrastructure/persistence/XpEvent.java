package ru.studex.user.infrastructure.persistence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum XpEvent {

    // ── Исполнитель ───────────────────────────────────────────
    TASK_COMPLETED    (50,  "Завершил задание"),
    TASK_FIRST        (100, "Первое выполненное задание — бонус"),
    REVIEW_RECEIVED   (20,  "Получил хороший отзыв (4–5 ★)"),
    PROPOSAL_ACCEPTED (10,  "Отклик принят заказчиком"),
    VERIFIED          (200, "Прошёл верификацию"),

    // ── Заказчик ──────────────────────────────────────────────
    TASK_CREATED      (10,  "Создал задание"),
    TASK_ACCEPTED     (15,  "Принял работу исполнителя"),
    TASK_FIRST_CREATED(50,  "Первое созданное задание — бонус"),

    // ── Общие ─────────────────────────────────────────────────
    DAILY_LOGIN       (5,   "Ежедневный вход на платформу"),
    PROFILE_COMPLETED (30,  "Заполнил профиль полностью"),
    REFERRAL          (100, "Пригласил друга");

    private final int amount;
    private final String description;

    /** Уровень по количеству XP (каждые 500 XP = новый уровень) */
    public static int levelFor(int xp) {
        return Math.max(1, xp / 500 + 1);
    }
}
