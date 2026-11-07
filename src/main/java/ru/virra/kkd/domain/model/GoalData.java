package ru.virra.kkd.domain.model;

import java.util.List;

public record GoalData(long id, GoalStatus status, String ownerId, List<KeyResultData> keyResults) {

    public GoalData {
        keyResults = List.copyOf(keyResults);
    }
}
