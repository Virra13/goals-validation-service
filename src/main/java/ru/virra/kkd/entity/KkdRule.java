package ru.virra.kkd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.virra.kkd.domain.model.Severity;

@Entity
@Table(name = "kkd_rule")
@Getter
@Setter
@NoArgsConstructor
public class KkdRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
