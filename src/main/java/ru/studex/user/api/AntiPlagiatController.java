package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.application.AntiPlagiatService;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AntiPlagiatController {

    private final AntiPlagiatService antiPlagiatService;

    /** Исполнитель запрашивает проверку оригинальности */
    @PostMapping("/{userId}/anti-plagiat/request")
    public ResponseEntity<Void> requestVerification(
            @PathVariable String userId,
            @AuthenticationPrincipal(errorOnInvalidType = false) String currentUserId) {
        if (currentUserId == null || !currentUserId.equals(userId))
            throw new ApiException("Нет доступа", 403);
        antiPlagiatService.requestVerification(UUID.fromString(userId));
        return ResponseEntity.ok().build();
    }

    /** Администратор подтверждает/отклоняет заявку */
    @PostMapping("/{userId}/anti-plagiat/verify")
    public ResponseEntity<Void> verify(
            @PathVariable String userId,
            @RequestParam boolean approved) {
        antiPlagiatService.processVerification(UUID.fromString(userId), approved);
        return ResponseEntity.ok().build();
    }
}
