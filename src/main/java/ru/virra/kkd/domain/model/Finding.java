package ru.virra.kkd.domain.model;

public record Finding(String ruleCode, IssueObjectType objectType,
                      long objectId, long goalId, Severity severity,
                      String description) {

    public static Finding forGoal(String ruleCode, long goalId,
                                  Severity severity, String description) {
        return new Finding(ruleCode, IssueObjectType.GOAL, goalId, goalId,
                severity, description);
    }
}
