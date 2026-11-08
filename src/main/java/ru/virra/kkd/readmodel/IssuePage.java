package ru.virra.kkd.readmodel;

import java.util.List;

/**
 * Модель чтения страницы проблем ККД.
 * Поле {@code total} содержит число проблем, соответствующих фильтрам,
 * до применения {@code limit} и {@code offset}.
 */
public record IssuePage(List<IssueListItem> content, int limit, int offset, long total) {
}
