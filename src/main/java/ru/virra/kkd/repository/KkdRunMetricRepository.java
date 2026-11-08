package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KkdRunMetric;

import java.util.List;

/**
 * Репозиторий сохранённых метрик прогонов ККД.
 * Предоставляет значения метрик по прогону и подразделению.
 */
public interface KkdRunMetricRepository extends JpaRepository<KkdRunMetric, Long> {
    List<KkdRunMetric> findAllByRunIdAndUnitId(Long runId, Long unitId);
}
