package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Сохранённое значение метрики для пары «прогон — подразделение».
 * Значение может быть {@code null}, если знаменатель метрики равен нулю.
 * Пара «прогон — подразделение — код метрики» уникальна.
 */
@Entity
@Table(
        name = "kkd_run_metric",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kkd_run_metric_run_unit_code",
                        columnNames = {
                                "run_id",
                                "unit_id",
                                "metric_code"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class KkdRunMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kkd_run_metric_id")
    @SequenceGenerator(name = "kkd_run_metric_id", sequenceName = "kkd_run_metric_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private KkdRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "metric_code", nullable = false, length = 100)
    private String metricCode;

    @Column(precision = 18, scale = 4)
    private BigDecimal value;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
