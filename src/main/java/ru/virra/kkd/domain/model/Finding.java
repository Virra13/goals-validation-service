package ru.virra.kkd.domain.model;

/**
 * Проблема ККД, обнаруженная при проверке правила.
 * Содержит тип и идентификатор объекта нарушения, а также идентификатор цели,
 * по которой определяется подразделение при сохранении проблемы.
 */
public record Finding(String ruleCode, IssueObjectType objectType,
                      long objectId, long goalId, IssueSeverity severity,
                      String description) {

    /**
     * Создаёт проблему, объектом которой является сама цель.
     *
     * @param ruleCode код нарушенного правила ККД
     * @param goalId идентификатор цели
     * @param severity важность проблемы
     * @param description описание обнаруженной проблемы
     * @return проблема с типом объекта {@code GOAL}
     */
    public static Finding forGoal(String ruleCode, long goalId,
                                  IssueSeverity severity, String description) {
        return new Finding(ruleCode, IssueObjectType.GOAL, goalId, goalId,
                severity, description);
    }
}
