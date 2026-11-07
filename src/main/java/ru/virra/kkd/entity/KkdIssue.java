package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.Severity;

import java.time.Instant;

@Entity
@Table(
        name = "kkd_issue",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kkd_issue_run_rule_object",
                        columnNames = {
                                "run_id",
                                "rule_id",
                                "object_type",
                                "object_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class KkdIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kkd_issue_id")
    @SequenceGenerator(name = "kkd_issue_id", sequenceName = "kkd_issue_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private KkdRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private KkdRule rule;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, length = 20)
    private IssueObjectType objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
}
