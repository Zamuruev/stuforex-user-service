package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartnerAdRepository extends JpaRepository<PartnerAdEntity, UUID> {

    // Все активные блоки отсортированные по sort_order
    List<PartnerAdEntity> findByActiveTrueOrderBySortOrderAsc();

    // Все блоки для админки
    List<PartnerAdEntity> findAllByOrderBySortOrderAscCreatedAtDesc();
}
