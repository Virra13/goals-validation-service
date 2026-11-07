package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.Severity;

public record IssueListItem(
        long id,
        String ruleCode,
        IssueObjectType objectType,
        long objectId,
        Severity severity,
        String description
) {
}
