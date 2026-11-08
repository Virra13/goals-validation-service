package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.virra.kkd.domain.model.RunStatus;

import java.time.Instant;

/**
 * Сохранённый прогон ККД для определённой даты снимка.
 * Содержит статус выполнения и необязательную ссылку на прогон для сравнения;
 * у первого прогона такая ссылка отсутствует.
 */
@Entity
@Table(name = "kkd_run")
@Getter
@Setter
@NoArgsConstructor
public class KkdRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @Column(name = "snapshot_date", nullable = false)
    private Instant snapshotDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compared_to_run_id")
    private KkdRun comparedToRun;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
