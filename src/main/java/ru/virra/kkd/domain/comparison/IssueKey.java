package ru.virra.kkd.domain.comparison;

import ru.virra.kkd.domain.model.IssueObjectType;

public record IssueKey(long ruleId, IssueObjectType objectType, long objectId) {
}
