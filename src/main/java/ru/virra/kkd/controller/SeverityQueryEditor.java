package ru.virra.kkd.controller;

import ru.virra.kkd.openapi.model.Severity;

import java.beans.PropertyEditorSupport;

final class SeverityQueryEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(Severity.fromValue(text));
    }
}
