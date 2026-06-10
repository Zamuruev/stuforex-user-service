package ru.studex.user.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class AdDtos {

    // ── Ответ ─────────────────────────────────────────────────

    @Data
    public static class AdResponse {
        private UUID id;
        private String company;
        private String tagline;
        private String description;
        private String cta;
        private String url;
        private String accentColor;
        private String bgColor;
        private String borderColor;
        private String logo;
        private Boolean active;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Создание ──────────────────────────────────────────────

    @Data
    public static class CreateAdRequest {
        @NotBlank(message = "Название компании обязательно")
        @Size(max = 100)
        private String company;

        @Size(max = 100)
        private String tagline;

        private String description;

        @Size(max = 50)
        private String cta = "Подробнее →";

        @NotBlank(message = "Ссылка обязательна")
        private String url;

        private String accentColor = "bg-violet-600 hover:bg-violet-700";
        private String bgColor     = "bg-violet-50 dark:bg-violet-900/10";
        private String borderColor = "border-violet-200 dark:border-violet-800/50";

        @Size(max = 10)
        private String logo;

        private Boolean active    = true;
        private Integer sortOrder = 0;
    }

    // ── Обновление (все поля опциональные) ───────────────────

    @Data
    public static class UpdateAdRequest {
        @Size(max = 100)
        private String company;

        @Size(max = 100)
        private String tagline;

        private String description;

        @Size(max = 50)
        private String cta;

        private String url;
        private String accentColor;
        private String bgColor;
        private String borderColor;

        @Size(max = 10)
        private String logo;

        private Boolean active;
        private Integer sortOrder;
    }
}
