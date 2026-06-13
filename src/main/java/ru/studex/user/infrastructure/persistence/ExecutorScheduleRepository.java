package ru.studex.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExecutorScheduleRepository extends JpaRepository<ExecutorScheduleEntity, UUID> {

    List<ExecutorScheduleEntity> findByUserIdOrderByDayOfWeek(UUID userId);

    @Modifying
    @Query("DELETE FROM ExecutorScheduleEntity s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
