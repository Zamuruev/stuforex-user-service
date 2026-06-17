package ru.studex.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studex.user.api.dto.AuthDtos;
import ru.studex.user.api.exception.ApiException;
import ru.studex.user.infrastructure.persistence.*;
import ru.studex.user.infrastructure.security.JwtProvider;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserJpaRepository userRepository;
    private final ReferralRepository referralRepository;
    private final XpService xpService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserMapper userMapper;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Пользователь с таким email уже существует", 400);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException("Имя пользователя уже занято", 400);
        }

        String role = (request.getRole() != null &&
                request.getRole().equalsIgnoreCase("EXECUTOR")) ? "EXECUTOR" : "STUDENT";

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .fullName(request.getFullName())
                .role(role)
                .isVerified(true) // MVP: auto-verify
                .build();

        user = userRepository.save(user);

        Optional<UserEntity> inviterOpt = userRepository.findByRefCode(request.getRefCode());

        if (inviterOpt.isPresent()) {
            UserEntity inviter = inviterOpt.get();

            if (!inviter.getId().equals(user.getId())) {

                referralRepository.save(
                        ReferralEntity.builder()
                                .inviterId(inviter.getId())
                                .inviteeId(user.getId())
                                .isActive(true)
                                .build()
                );

                try {
                    notificationService.notifyReferralRegistered(
                            inviter.getId(),
                            user.getUsername()
                    );
                } catch (Exception e) {
                    log.warn("Не удалось отправить уведомление о реферале inviterId={}: {}",
                            inviter.getId(), e.getMessage());
                }

            }
        }

        log.info("Registered user: {} as {}", user.getEmail(), role);

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        // Начислить XP за вход ДО формирования ответа — чтобы фронт получил актуальные данные
        xpService.award(user.getId(), XpEvent.DAILY_LOGIN, user.getId().toString());

        // Перечитать пользователя из БД — чтобы вернуть актуальные xpPoints и level
        user = userRepository.findById(user.getId()).orElse(user);

        return new AuthDtos.AuthResponse(token, userMapper.toResponse(user));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Неверный email или пароль", 401));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException("Неверный email или пароль", 401);
        }

        if (!user.getIsActive()) {
            throw new ApiException("Аккаунт заблокирован", 403);
        }

        String token = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        // Начислить XP за вход ДО формирования ответа — чтобы фронт получил актуальные данные
        xpService.award(user.getId(), XpEvent.DAILY_LOGIN, user.getId().toString());

        // Перечитать пользователя из БД — чтобы вернуть актуальные xpPoints и level
        user = userRepository.findById(user.getId()).orElse(user);

        return new AuthDtos.AuthResponse(token, userMapper.toResponse(user));
    }

    public AuthDtos.UserResponse getMe(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("Пользователь не найден", 404));
        
        if (user.isDeleted() || Boolean.FALSE.equals(user.getIsActive())) {
            throw new ApiException("Аккаунт деактивирован", 401);
        }

        return userMapper.toResponse(user);
    }
}
