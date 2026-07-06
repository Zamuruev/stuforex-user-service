package ru.studex.user.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.application.AuthService;
import ru.studex.user.application.OnlinePresenceService;
import ru.studex.user.application.VerificationService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OnlinePresenceService presenceService;
    private final VerificationService verificationService;

    /** Отправить код на email ДО регистрации */
    @PostMapping("/email/send-code")
    public ResponseEntity<Map<String, String>> sendPreRegCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email обязателен"));
        verificationService.sendPreRegCode(email.trim().toLowerCase());
        return ResponseEntity.ok(Map.of("message", "Код отправлен на " + email));
    }

    /** Подтвердить email ДО регистрации */
    @PostMapping("/email/verify-code")
    public ResponseEntity<Map<String, Object>> verifyPreRegCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code  = body.get("code");
        if (email == null || code == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email и код обязательны"));
        boolean ok = verificationService.verifyPreRegCode(email.trim().toLowerCase(), code.trim());
        if (!ok) return ResponseEntity.badRequest().body(Map.of("error", "Неверный или истёкший код"));
        return ResponseEntity.ok(Map.of("verified", true));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {
        log.info("Register attempt: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());
        // XP начисляется внутри authService.login() до формирования ответа
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> me(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.getMe(userId));
    }

    /** Heartbeat — продлевает TTL присутствия в Redis */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal String userId) {
        presenceService.recordActivity(userId);
        return ResponseEntity.ok().build();
    }

    /** Logout — мгновенно убирает пользователя из онлайна */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal String userId) {
        if (userId != null) presenceService.removeActivity(userId);
        return ResponseEntity.ok().build();
    }
}
