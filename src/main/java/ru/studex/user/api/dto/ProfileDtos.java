package ru.studex.user.api.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class ProfileDtos {

    @Data
    public static class AchievementDto {
        private String id;
        private String title;
        private String desc;
        private String icon;   // название иконки для фронта
        private String color;  // цветовой акцент
        private boolean unlocked;
    }

    @Data
    public static class ReferralDto {
        private String id;
        private String username;
        private String role;
        private Boolean isActive;
        private LocalDateTime joinedAt;
    }

    @Data
    public static class ReferralStatsDto {
        private String code;
        private int totalInvited;
        private int activeUsers;
        private String pendingReward; // null если нет
        private List<ReferralDto> referrals;
    }
}
