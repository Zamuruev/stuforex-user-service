package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.UserEntity;
import ru.studex.user.infrastructure.persistence.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

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
        if (updates.containsKey("telegram")) user.setTelegram((String) updates.get("telegram"));
        if (updates.containsKey("specialization")) user.setSpecialization((String) updates.get("specialization"));
        if (updates.containsKey("skills") && updates.get("skills") instanceof List<?> s) {
            String[] arr = s.stream().map(Object::toString).toArray(String[]::new);
            user.setSkillsArray(arr);
        }
        if (updates.containsKey("courseYear")) {
            user.setCourseYear(updates.get("courseYear") != null
                    ? Short.valueOf(updates.get("courseYear").toString())
                    : null);
        }
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (!newEmail.equals(user.getEmail())) {
                user.setEmail(newEmail);
                user.setEmailVerified(false);
            }
        }
        if (updates.containsKey("phone")) {
            String newPhone = (String) updates.get("phone");
            if (newPhone != null && !newPhone.equals(user.getPhone())) {
                user.setPhone(newPhone);
                user.setPhoneVerified(false);
            }
        }
        if (updates.containsKey("coverPositionY")) {
            user.setCoverPositionY(updates.get("coverPositionY") != null
                    ? ((Number) updates.get("coverPositionY")).intValue()
                    : 50);
        }
        if (updates.containsKey("monthlyBudgetLimit")) {
            user.setMonthlyBudgetLimit(updates.get("monthlyBudgetLimit") != null
                    ? ((Number) updates.get("monthlyBudgetLimit")).intValue()
                    : null);
        }
        if (updates.containsKey("username")) {
            String newUsername = ((String) updates.get("username")).trim().toLowerCase();
            if (!newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
                if (newUsername.length() < 3 || newUsername.length() > 30)
                    throw new ApiException("Никнейм должен быть от 3 до 30 символов", 400);
                if (!newUsername.matches("[a-z0-9_]+"))
                    throw new ApiException("Никнейм может содержать только латинские буквы, цифры и _", 400);
                if (userRepository.existsByUsername(newUsername))
                    throw new ApiException("Этот никнейм уже занят", 409);
                user.setUsername(newUsername);
            }
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

    @Transactional
    public void changePassword(String userId, String newPassword) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException("Новый пароль совпадает с текущим", 400);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user {}", userId);
    }

    @Transactional
    public void deleteAccount(String userId, String password) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException("Неверный пароль", 400);
        }

        user.setIsActive(false);
        user.setDeleted(true);
        user.setEmail("deleted_" + userId + "@deleted.studex");
        user.setUsername("deleted_" + userId.substring(0, 8));
        user.setFullName(null);
        user.setPhone(null);
        user.setTelegram(null);
        user.setBio(null);
        user.setAvatarUrl(null);
        userRepository.save(user);

        log.info("Account soft-deleted for user {}", userId);
    }

    @Transactional
    public AuthDtos.UserResponse uploadAvatar(String userId, MultipartFile file) {
        UserEntity user = findUser(userId);
        // Delete old avatar file if stored locally
        if (user.getAvatarUrl() != null && user.getAvatarUrl().startsWith("/uploads/")) {
            fileStorageService.delete(user.getAvatarUrl());
        }
        String url = fileStorageService.storeAvatar(userId, file);
        user.setAvatarUrl(url);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.UserResponse deleteAvatar(String userId) {
        UserEntity user = findUser(userId);
        if (user.getAvatarUrl() != null && user.getAvatarUrl().startsWith("/uploads/")) {
            fileStorageService.delete(user.getAvatarUrl());
        }
        user.setAvatarUrl(null);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.UserResponse uploadCover(String userId, MultipartFile file) {
        UserEntity user = findUser(userId);
        if (user.getCoverUrl() != null && user.getCoverUrl().startsWith("/uploads/")) {
            fileStorageService.delete(user.getCoverUrl());
        }
        String url = fileStorageService.storeCover(userId, file);
        user.setCoverUrl(url);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDtos.UserResponse deleteCover(String userId) {
        UserEntity user = findUser(userId);
        if (user.getCoverUrl() != null && user.getCoverUrl().startsWith("/uploads/")) {
            fileStorageService.delete(user.getCoverUrl());
        }
        user.setCoverUrl(null);
        return userMapper.toResponse(userRepository.save(user));
    }

    public long getTotalUsersCount() {
        return userRepository.countActiveUsers();
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
    }
}
