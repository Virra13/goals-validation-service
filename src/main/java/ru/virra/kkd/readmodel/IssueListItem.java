package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.IssueSeverity;

/**
 * Модель чтения одной проблемы в ответе списка проблем ККД.
 * Содержит поля, необходимые для отображения проблемы без загрузки связанного прогона
 * и записи справочника правил.
 */
public record IssueListItem(
        long id,
        String ruleCode,
        IssueObjectType objectType,
        long objectId,
        IssueSeverity severity,
        String description
) {
}
