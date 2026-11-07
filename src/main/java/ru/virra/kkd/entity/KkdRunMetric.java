package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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