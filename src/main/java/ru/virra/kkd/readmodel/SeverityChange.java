package ru.virra.kkd.readmodel;

import ru.virra.kkd.domain.model.Severity;

public record SeverityChange(Severity severity, long current, Long previous, Long delta) {
}
