package ru.virra.kkd.domain.model;

import java.util.List;

/**
 * Данные цели и её ключевых результатов для проверки правил ККД и расчёта метрик.
 * Список ключевых результатов копируется при создании записи, чтобы расчёты
 * работали с неизменяемым набором данных цели.
 */
public record GoalData(long id, GoalStatus status, String ownerId, List<KeyResultData> keyResults) {

    public GoalData {
        keyResults = List.copyOf(keyResults);
    }
}
