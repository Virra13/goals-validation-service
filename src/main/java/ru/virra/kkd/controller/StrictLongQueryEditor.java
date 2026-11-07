package ru.virra.kkd.controller;

import java.beans.PropertyEditorSupport;

/** Missing optional parameters are allowed; supplied empty or repeated values are invalid. */
final class StrictLongQueryEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        if (text == null || !text.matches("[0-9]+")) {
            throw new IllegalArgumentException("Ожидается один целочисленный ID");
        }
        setValue(Long.valueOf(text));
    }
}
