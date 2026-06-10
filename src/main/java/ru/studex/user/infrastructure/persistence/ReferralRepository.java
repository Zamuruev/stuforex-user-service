package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {
    List<ReferralEntity> findByInviterIdOrderByCreatedAtDesc(UUID inviterId);
    Optional<ReferralEntity> findByInviteeId(UUID inviteeId);
    @Query("SELECT COUNT(r) FROM ReferralEntity r WHERE r.inviterId = :inviterId AND r.isActive = true")
    long countActiveByInviterId(UUID inviterId);
    @Query("SELECT COUNT(r) FROM ReferralEntity r WHERE r.inviterId = :inviterId")
    long countByInviterId(UUID inviterId);
}
