package ru.virra.kkd.readmodel;

import java.time.LocalDate;

/**
 * Ссылка на прогон ККД в модели чтения сводной статистики.
 * Содержит числовой идентификатор и календарную дату снимка в UTC.
 */
public record RunReference(long id, LocalDate snapshotDate) {
}
