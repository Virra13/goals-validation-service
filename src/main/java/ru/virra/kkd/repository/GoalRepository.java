package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.domain.model.GoalStatus;
import ru.virra.kkd.entity.Goal;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByStatus(GoalStatus status);
    List<Goal> findAllByUnitIdAndStatus(Long unitId,GoalStatus status);
}
