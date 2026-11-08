package ru.virra.kkd.repository.projection;

import ru.virra.kkd.domain.model.RunStatus;

import java.time.Instant;

/**
 * Проекция прогона для списка прогонов ККД.
 * Содержит дату снимка, статус, ссылку на прогон для сравнения
 * и количество найденных проблем без загрузки коллекции проблем.
 */
public record RunListRow(
        Long id,
        RunStatus status,
        Instant snapshotDate,
        Long comparedToRunId,
        Long issuesCount
) {
}
