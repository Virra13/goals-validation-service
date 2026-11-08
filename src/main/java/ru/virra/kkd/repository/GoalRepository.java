package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.entity.Goal;

import java.util.List;

/**
 * Репозиторий целей для выполнения прогона ККД.
 * Позволяет выбирать цели по статусу для выполнения проверки.
 */
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByStatus(GoalStatus status);
}
