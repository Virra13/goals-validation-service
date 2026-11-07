package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KeyResult;
import ru.virra.kkd.domain.model.GoalStatus;

import java.util.List;

public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {
    List<KeyResult> findAllByGoalStatus(GoalStatus status);
    List<KeyResult> findAllByGoalId(Long goalId);
    List<KeyResult> findAllByGoalIdIn(List<Long> goalIds);
}
