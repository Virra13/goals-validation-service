package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KeyResult;
import ru.virra.kkd.domain.model.GoalStatus;

import java.util.List;

/**
 * Репозиторий ключевых результатов целей.
 * Позволяет выбирать ключевые результаты по статусу связанной цели.
 */
public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {
    List<KeyResult> findAllByGoalStatus(GoalStatus status);
}
