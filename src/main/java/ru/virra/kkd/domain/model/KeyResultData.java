package ru.virra.kkd.domain.model;

import java.time.LocalDate;

public record KeyResultData(long id, Integer weight, LocalDate lastFactUpdate) {
}
