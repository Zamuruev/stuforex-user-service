package ru.studex.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @Email(message = "Некорректный email")
        @NotBlank(message = "Email обязателен")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, message = "Пароль минимум 6 символов")
        private String password;

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 50, message = "Имя пользователя 3-50 символов")
        private String username;

        private String fullName;
        private String role;
        private String refCode;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email обязателен")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private UserResponse user;

        public AuthResponse(String accessToken, UserResponse user) {
            this.accessToken = accessToken;
            this.user = user;
        }
    }

    @Data
    public static class UserResponse {
        private String id;
        private String email;
        private String username;
        private String fullName;
        private String avatarUrl;
        private String role;
        private Boolean isVerified;
        private Boolean isActive;
        private String bio;
        private String university;
        private Integer xpPoints;
        private Integer level;
        private String phone;
        private String telegram;
        private List<String> skills;
        private String specialization;
        private String subscriptionPlan;
        private String refCode;
        private Boolean emailVerified;
        private Boolean phoneVerified;
        private Short courseYear;
        private Integer monthlyBudgetLimit;
    }

    @Data
    public static class UpdateProfileRequest {
        private String fullName;
        private String bio;
        private String university;
        private String avatarUrl;
        private String phone;
        private String telegram;
        private List<String> skills;
        private String specialization;
    }

    @Data
    public static class AdminUserListResponse {
        private List<UserResponse> users;
        private long total;
        private int page;
        private int size;
    }
}
