package ru.virra.kkd.domain.comparison;

/**
 * Результат классификации проблем при сравнении двух прогонов ККД.
 * Содержит количество новых, сохраняющихся и исправленных проблем;
 * классификация основана на идентичности проблемы, а не на её описании или важности.
 */
public record IssueDelta(long newCount, long persisting, long resolved) {
}
