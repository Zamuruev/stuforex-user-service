package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProviderEntity, UUID> {
    Optional<UserOAuthProviderEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
