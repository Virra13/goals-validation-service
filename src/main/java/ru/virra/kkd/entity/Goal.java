package ru.virra.kkd.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.virra.kkd.domain.model.GoalStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "goal")
@Getter
@Setter
@NoArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_goal_id")
    private Goal parentGoal;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "owner_id", length = 100)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    private LocalDate deadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
