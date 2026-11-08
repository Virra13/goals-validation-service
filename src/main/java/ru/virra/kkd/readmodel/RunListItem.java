package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.RunStatus;

import java.time.LocalDate;

/**
 * Модель чтения одного прогона ККД для списка прогонов.
 * Содержит дату снимка в UTC и количество найденных проблем.
 * Идентификатор прогона для сравнения равен {@code null}, если сравнения нет.
 */
public record RunListItem(
        long id,
        RunStatus status,
        LocalDate snapshotDate,
        Long comparedToRunId,
        long issuesCount
) {
}
