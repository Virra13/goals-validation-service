package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ключевой результат, связанный с целью.
 * Вес и дата последнего обновления факта могут отсутствовать;
 * соответствующие нарушения определяются правилами ККД.
 */
@Entity
@Table(name = "key_result")
@Getter
@Setter
@NoArgsConstructor
public class KeyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "target_value", precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "fact_value", precision = 18, scale = 4)
    private BigDecimal factValue;

    @Column
    private Integer weight;

    @Column(name = "last_fact_update")
    private Instant lastFactUpdate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
