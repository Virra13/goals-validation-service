package ru.virra.kkd.domain.model;

public final class GoalQuality {
    private GoalQuality() {
    }

    public static boolean hasOwner(GoalData goal) {
        return goal.ownerId() != null && !goal.ownerId().isBlank();
    }

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
