package ru.virra.kkd.domain.model;

/**
 * Общие предикаты качества данных цели для правил ККД и расчёта метрик.
 * Использование одних условий исключает различия между проверкой нарушений
 * и вычислением долей целей с ответственным или корректной суммой весов КР.
 */
public final class GoalQuality {
    private GoalQuality() {
    }

    /**
     * Проверяет наличие ответственного у цели по условию правила {@code GOAL_NO_OWNER}.
     *
     * @param goal проверяемая цель
     * @return {@code true}, если идентификатор ответственного задан и не состоит из пробелов
     */
    public static boolean hasOwner(GoalData goal) {
        return goal.ownerId() != null && !goal.ownerId().isBlank();
    }

    /**
     * Проверяет условие корректной суммы весов ключевых результатов.
     * У цели должен быть хотя бы один КР, каждый его вес должен быть задан,
     * а сумма всех весов должна равняться 100.
     *
     * @param goal проверяемая цель
     * @return {@code true}, если все условия выполнены
     */
    public static boolean hasValidWeights(GoalData goal) {
        if (goal.keyResults().isEmpty()) {
            return false;
        }
        long sum = 0;
        for (KeyResultData result : goal.keyResults()) {
            if (result.weight() == null) {
                return false;
            }
            sum += result.weight();
        }
        return sum == 100;
    }
}
