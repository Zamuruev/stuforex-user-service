package ru.studex.user.api.dto;

import lombok.Data;
import java.util.Map;

public class NotificationDtos {

    @Data
    public static class NotificationResponse {
        private String id;
        private String type;
        private String title;
        private String body;
        private Boolean isRead;
        private String link;
        private Map<String, Object> meta;
        private String createdAt;
    }
}
