package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserJpaRepository userRepository;

    // ── Email ─────────────────────────────────────────────────

    @Transactional
    public void sendEmailCode(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String code = generateCode();
        user.setEmailCode(code);
        user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // TODO: подключить реальный email-провайдер (Spring Mail / SendGrid)
        // Пока логируем код для разработки
        log.info("=== EMAIL VERIFICATION CODE для {} === {}", user.getEmail(), code);
    }

    @Transactional
    public boolean confirmEmail(String userId, String code) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (code == null || !code.equals(user.getEmailCode())) return false;
        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) return false;

        user.setEmailVerified(true);
        user.setEmailCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    // ── Phone ─────────────────────────────────────────────────

    @Transactional
    public void sendPhoneCode(String userId, String phone) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Обновляем телефон если передан
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }

        String code = generateCode();
        user.setPhoneCode(code);
        user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // TODO: подключить SMS-провайдер (СМСК, СМСЦ, СМС Центр)
        log.info("=== SMS VERIFICATION CODE для {} === {}", user.getPhone(), code);
    }

    @Transactional
    public boolean confirmPhone(String userId, String code) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (code == null || !code.equals(user.getPhoneCode())) return false;
        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) return false;

        user.setPhoneVerified(true);
        user.setPhoneCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    // ── Helper ────────────────────────────────────────────────

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
