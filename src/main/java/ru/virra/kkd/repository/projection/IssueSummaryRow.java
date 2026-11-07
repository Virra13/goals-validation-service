package ru.virra.kkd.repository.projection;

import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.Severity;

public record IssueSummaryRow(long ruleId, IssueObjectType objectType, long objectId, Severity severity) {
}
