package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.application.VerificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/verify")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /** Отправить код на email */
    @PostMapping("/email/send")
    public ResponseEntity<Map<String, String>> sendEmailCode(
            @AuthenticationPrincipal String userId) {
        verificationService.sendEmailCode(userId);
        return ResponseEntity.ok(Map.of("message", "Код отправлен на email"));
    }

    /** Подтвердить email кодом */
    @PostMapping("/email/confirm")
    public ResponseEntity<Map<String, Object>> confirmEmail(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {
        boolean ok = verificationService.confirmEmail(userId, body.get("code"));
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "Неверный или истёкший код"));
        return ResponseEntity.ok(Map.of("emailVerified", true));
    }

    /** Отправить код на телефон (SMS) */
    @PostMapping("/phone/send")
    public ResponseEntity<Map<String, String>> sendPhoneCode(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        verificationService.sendPhoneCode(userId, phone);
        return ResponseEntity.ok(Map.of("message", "Код отправлен на телефон"));
    }

    /** Подтвердить телефон кодом */
    @PostMapping("/phone/confirm")
    public ResponseEntity<Map<String, Object>> confirmPhone(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {
        boolean ok = verificationService.confirmPhone(userId, body.get("code"));
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "Неверный или истёкший код"));
        return ResponseEntity.ok(Map.of("phoneVerified", true));
    }
}
