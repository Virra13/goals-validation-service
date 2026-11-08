package ru.virra.kkd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.readmodel.RunListItem;
import ru.virra.kkd.repository.KkdRunRepository;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Формирует модель чтения списка прогонов ККД.
 * Для каждого прогона получает статус, дату снимка, идентификатор прогона
 * для сравнения и количество найденных проблем.
 */
@Service
@Slf4j
public class KkdRunQueryService {

    private final KkdRunRepository runs;

    public KkdRunQueryService(KkdRunRepository runs) {
        this.runs = runs;
    }

    /**
     * Возвращает список прогонов в порядке убывания даты снимка и идентификатора.
     * Дата снимка преобразуется из {@code Instant} в календарную дату UTC.
     *
     * @return модели чтения прогонов с количеством найденных проблем
     */
    @Transactional(readOnly = true)
    public List<RunListItem> listRuns() {
        List<RunListItem> result = runs.findRunList().stream()
                .map(row -> new RunListItem(
                        row.id(),
                        row.status(),
                        row.snapshotDate().atOffset(ZoneOffset.UTC).toLocalDate(),
                        row.comparedToRunId(),
                        row.issuesCount()))
                .toList();
        log.info("Получен список прогонов ККД: количество={}", result.size());
        return result;
    }
}
