package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KkdRunMetric;

import java.util.List;
import java.util.Optional;

public interface KkdRunMetricRepository extends JpaRepository<KkdRunMetric, Long> {
    List<KkdRunMetric> findAllByRunIdAndUnitId(Long runId, Long unitId);
    Optional<KkdRunMetric> findByRunIdAndUnitIdAndMetricCode(Long runId, Long unitId, String metricCode);
}