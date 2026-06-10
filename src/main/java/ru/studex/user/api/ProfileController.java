package ru.studex.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.studex.user.api.dto.ProfileDtos;
import ru.studex.user.application.ProfileService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** Активность за 365 дней: { "2026-05-20": 3, "2026-06-01": 1, ... } */
    @GetMapping("/{userId}/activity")
    public ResponseEntity<Map<String, Integer>> getActivity(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.getActivity(userId));
    }

    /** Достижения пользователя */
    @GetMapping("/{userId}/achievements")
    public ResponseEntity<List<ProfileDtos.AchievementDto>> getAchievements(@PathVariable String userId) {
        return ResponseEntity.ok(profileService.getAchievements(userId));
    }

    /** Реферальная статистика — только для своего профиля */
    @GetMapping("/referrals/my")
    public ResponseEntity<ProfileDtos.ReferralStatsDto> getMyReferrals(
            @AuthenticationPrincipal(errorOnInvalidType = false) String userId) {
        return ResponseEntity.ok(profileService.getReferralStats(userId));
    }

    /** Пометить реферала активным — вызывается task-service при первом действии */
    @PostMapping("/referrals/{inviteeId}/activate")
    public ResponseEntity<Void> activateReferral(@PathVariable String inviteeId) {
        profileService.markReferralActive(inviteeId);
        return ResponseEntity.ok().build();
    }
}
