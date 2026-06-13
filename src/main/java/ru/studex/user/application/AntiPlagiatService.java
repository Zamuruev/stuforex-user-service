package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntiPlagiatService {

    private final UserJpaRepository userRepository;

    /**
     * Исполнитель подаёт заявку на проверку оригинальности.
     * Повторная заявка разрешена только если предыдущая была отклонена (verified=false).
     */
    @Transactional
    public void requestVerification(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        if (!user.getRole().equals("EXECUTOR"))
            throw new ApiException("Только исполнители могут запросить верификацию", 403);

        if (Boolean.TRUE.equals(user.getAntiPlagiatVerified()))
            throw new ApiException("Уже верифицирован", 400);

        // Не разрешаем повторную заявку если предыдущая ещё не рассмотрена
        if (user.getAntiPlagiatRequestedAt() != null
                && Boolean.FALSE.equals(user.getAntiPlagiatVerified())) {
            // Проверяем что заявка не старше 30 дней (после отказа можно подать снова)
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            if (user.getAntiPlagiatRequestedAt().isAfter(threshold))
                throw new ApiException("Заявка уже находится на рассмотрении", 400);
        }

        user.setAntiPlagiatRequestedAt(LocalDateTime.now());
        // Сбрасываем флаг на null — означает "на рассмотрении"
        user.setAntiPlagiatVerified(null);
        userRepository.save(user);

        log.info("Anti-plagiat verification requested by userId={}", userId);
    }

    /**
     * Администратор одобряет или отклоняет заявку.
     */
    @Transactional
    public void processVerification(UUID userId, boolean approved) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        if (user.getAntiPlagiatRequestedAt() == null)
            throw new ApiException("Заявка не подавалась", 400);

        user.setAntiPlagiatVerified(approved);
        if (approved) {
            user.setAntiPlagiatVerifiedAt(LocalDateTime.now());
            log.info("Anti-plagiat APPROVED for userId={}", userId);
        } else {
            user.setAntiPlagiatVerifiedAt(null);
            log.info("Anti-plagiat REJECTED for userId={}", userId);
        }

        userRepository.save(user);
    }
}
