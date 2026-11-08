package ru.virra.kkd.domain.model;

import java.time.LocalDate;

/**
 * Данные ключевого результата для проверки правил ККД.
 * Вес и дата последнего обновления фактического значения могут быть {@code null};
 * их отсутствие проверяют правила {@code KR_WEIGHTS_SUM} и {@code KR_STALE_PROGRESS}.
 */
public record KeyResultData(long id, Integer weight, LocalDate lastFactUpdate) {
}
