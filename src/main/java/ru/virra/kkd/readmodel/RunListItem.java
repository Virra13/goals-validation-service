package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.RunStatus;

import java.time.LocalDate;

public record RunListItem(
        long id,
        RunStatus status,
        LocalDate snapshotDate,
        Long comparedToRunId,
        long issuesCount
) {
}
