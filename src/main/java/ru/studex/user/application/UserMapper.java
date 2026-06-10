package ru.studex.user.application;

import org.springframework.stereotype.Component;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.infrastructure.persistence.UserEntity;

import java.util.List;

@Component
public class UserMapper {
    public AuthDtos.UserResponse toResponse(UserEntity user) {
        AuthDtos.UserResponse r = new AuthDtos.UserResponse();
        r.setId(user.getId().toString());
        r.setEmail(user.getEmail());
        r.setUsername(user.getUsername());
        r.setFullName(user.getFullName());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setRole(user.getRole());
        r.setIsVerified(user.getIsVerified());
        r.setIsActive(user.getIsActive());
        r.setBio(user.getBio());
        r.setUniversity(user.getUniversity());
        r.setXpPoints(user.getXpPoints());
        r.setLevel(user.getLevel());
        r.setPhone(user.getPhone());
        r.setTelegram(user.getTelegram());
        r.setSkills(List.of(user.getSkills()));
        r.setSpecialization(user.getSpecialization());
        r.setSubscriptionPlan(
                user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "LITE"
        );
        r.setRefCode(user.getRefCode());
        return r;
    }
}
