package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    public AuthDtos.UserResponse getById(String id) {
        UserEntity user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
        return userMapper.toResponse(user);
    }

    @Transactional
    public AuthDtos.UserResponse updateProfile(String userId, Map<String, Object> updates) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        if (updates.containsKey("fullName")) user.setFullName((String) updates.get("fullName"));
        if (updates.containsKey("bio")) user.setBio((String) updates.get("bio"));
        if (updates.containsKey("university")) user.setUniversity((String) updates.get("university"));
        if (updates.containsKey("avatarUrl")) user.setAvatarUrl((String) updates.get("avatarUrl"));
        if (updates.containsKey("phone")) user.setPhone((String) updates.get("phone"));
        if (updates.containsKey("telegram")) user.setTelegram((String) updates.get("telegram"));
        if (updates.containsKey("specialization")) user.setSpecialization((String) updates.get("specialization"));
        if (updates.containsKey("skills") && updates.get("skills") instanceof List<?> s) {
            String[] arr = s.stream().map(Object::toString).toArray(String[]::new);
            user.setSkillsArray(arr);
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    // Старый search — для совместимости
    public List<AuthDtos.UserResponse> search(String query, String role) {
        String q = (query != null && !query.isBlank()) ? query : null;
        String r = (role != null && !role.isBlank()) ? role : null;
        return userRepository.adminSearch(q, r, 20, 0, null)
                .stream().map(userMapper::toResponse).toList();
    }

    // Для admin panel с пагинацией и фильтром по isActive
    public AuthDtos.AdminUserListResponse adminSearchUsers(
            String query, String role, int page, int size, Boolean isActive) {
        String q = (query != null && !query.isBlank()) ? query : null;
        String r = (role != null && !role.isBlank()) ? role : null;
        long offset = (long) page * size;

        List<AuthDtos.UserResponse> users = userRepository.adminSearch(q, r, size, offset, isActive)
                .stream().map(userMapper::toResponse).toList();
        long total = userRepository.adminSearchCount(q, r, isActive);

        AuthDtos.AdminUserListResponse resp = new AuthDtos.AdminUserListResponse();
        resp.setUsers(users);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return resp;
    }

    @Transactional
    public AuthDtos.UserResponse banUser(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
        user.setIsActive(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.UserResponse unbanUser(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
        user.setIsActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    public List<AuthDtos.UserResponse> getTopExecutors(int limit) {
        return userRepository.findTopExecutors(Math.min(limit, 10))
                .stream().map(userMapper::toResponse).toList();
    }

    public Map<String, Object> getTaskLimit(String userId) {
        return userRepository.findById(UUID.fromString(userId)).map(user -> {
            if (user.getProposalsResetAt() != null
                    && user.getProposalsResetAt().isBefore(LocalDateTime.now())) {
                user.setProposalsThisMonth(0);
                user.setProposalsResetAt(LocalDateTime.now().plusMonths(1));
                userRepository.save(user);
            }
            Integer limit = user.getTasksLimit();
            int used = user.getProposalsThisMonth() != null ? user.getProposalsThisMonth() : 0;
            boolean allowed = limit == null || used < limit;
            Map<String, Object> result = new HashMap<>();
            result.put("allowed", allowed);
            result.put("used", used);
            result.put("limit", limit);
            result.put("plan", user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "LITE");
            return result;
        }).orElse(Map.of("allowed", true, "used", 0, "limit", 3, "plan", "LITE"));
    }

    @Transactional
    public void incrementTaskCounter(String userId) {
        userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
            int current = user.getProposalsThisMonth() != null ? user.getProposalsThisMonth() : 0;
            user.setProposalsThisMonth(current + 1);
            if (user.getProposalsResetAt() == null) {
                user.setProposalsResetAt(LocalDateTime.now().plusMonths(1));
            }
            userRepository.save(user);
        });
    }

    public Map<String, Object> getProposalLimit(String userId) {
        return userRepository.findById(UUID.fromString(userId)).map(user -> {
            if (user.getProposalsResetAt() != null
                    && user.getProposalsResetAt().isBefore(LocalDateTime.now())) {
                user.setProposalsThisMonth(0);
                user.setProposalsResetAt(LocalDateTime.now().plusMonths(1));
                userRepository.save(user);
            }
            Integer limit = user.getProposalsLimit();
            int used = user.getProposalsThisMonth() != null ? user.getProposalsThisMonth() : 0;
            boolean allowed = limit == null || used < limit;
            Map<String, Object> result = new HashMap<>();
            result.put("allowed", allowed);
            result.put("used", used);
            result.put("limit", limit);
            result.put("plan", user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "LITE");
            return result;
        }).orElse(Map.of("allowed", true, "used", 0, "limit", 5, "plan", "LITE"));
    }

    @Transactional
    public void incrementProposalCounter(String userId) {
        userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
            int current = user.getProposalsThisMonth() != null ? user.getProposalsThisMonth() : 0;
            user.setProposalsThisMonth(current + 1);
            if (user.getProposalsResetAt() == null) {
                user.setProposalsResetAt(LocalDateTime.now().plusMonths(1));
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public void updateSubscription(String userId, String planName, String expiresAtStr) {
        userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
            if (planName != null)     user.setSubscriptionPlan(planName);
            if (expiresAtStr != null) user.setSubscriptionExpiresAt(LocalDateTime.parse(expiresAtStr));
            user.setProposalsThisMonth(0);
            user.setProposalsResetAt(LocalDateTime.now().plusMonths(1));
            userRepository.save(user);
        });
    }
}
