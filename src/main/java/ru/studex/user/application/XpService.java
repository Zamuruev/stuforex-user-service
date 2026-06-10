package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;
import ru.studex.user.infrastructure.persistence.XpEvent;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class XpService {

    private final UserJpaRepository userRepository;

    /**
     * Начислить XP пользователю за событие.
     * contextId используется для логирования — дедупликация на уровне бизнес-логики
     * (например, один раз за одно задание) обеспечивается вызывающим кодом.
     */
    @Transactional
    public void award(String userId, XpEvent event, String contextId) {
        try {
            award(UUID.fromString(userId), event, contextId);
        } catch (IllegalArgumentException e) {
            log.warn("Некорректный userId для начисления XP: {}", userId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void award(UUID userId, XpEvent event, String contextId) {
        userRepository.findById(userId).ifPresentOrElse(user -> {

            // Защита от повторного начисления DAILY_LOGIN в тот же день
            if (event == XpEvent.DAILY_LOGIN) {
                java.time.LocalDate today = java.time.LocalDate.now();
                if (today.equals(user.getLastDailyLogin())) {
                    log.debug("DAILY_LOGIN уже начислен сегодня для {}", userId);
                    return;
                }
                user.setLastDailyLogin(today);
            }

            int before = user.getXpPoints() != null ? user.getXpPoints() : 0;
            int after  = before + event.getAmount();
            int levelBefore = XpEvent.levelFor(before);
            int levelAfter  = XpEvent.levelFor(after);

            user.setXpPoints(after);
            user.setLevel(levelAfter);
            userRepository.save(user);

            log.info("XP +{} [{}] → {} (ctx={}): {}→{} XP, lvl {}→{}",
                    event.getAmount(), event.name(), userId, contextId,
                    before, after, levelBefore, levelAfter);

            if (levelAfter > levelBefore) {
                log.info("🎉 Пользователь {} достиг уровня {}!", userId, levelAfter);
                // TODO: уведомление через notification-service
            }
        }, () -> log.warn("Пользователь {} не найден для начисления XP", userId));
    }

    /** Без contextId */
    @Transactional
    public void award(String userId, XpEvent event) {
        award(userId, event, "global");
    }

    @Transactional
    public void award(UUID userId, XpEvent event) {
        award(userId, event, "global");
    }
}
