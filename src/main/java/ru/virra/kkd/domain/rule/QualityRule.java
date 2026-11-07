package ru.virra.kkd.domain.rule;

import ru.virra.kkd.domain.model.Finding;
import ru.virra.kkd.domain.model.GoalData;

import java.time.LocalDate;
import java.util.List;

interface QualityRule {
    List<Finding> check(GoalData goal, LocalDate snapshotDate);
}
