package ru.studex.user.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "executor_schedule",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "day_of_week"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExecutorScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 1=Пн .. 7=Вс */
    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "time_from", nullable = false)
    private LocalTime timeFrom;

    @Column(name = "time_to", nullable = false)
    private LocalTime timeTo;
}
