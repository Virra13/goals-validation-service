package ru.virra.kkd.repository.projection;

import ru.virra.kkd.domain.model.RunStatus;

import java.time.Instant;

public record RunListRow(
        Long id,
        RunStatus status,
        Instant snapshotDate,
        Long comparedToRunId,
        Long issuesCount
) {
}
