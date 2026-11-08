package ru.virra.kkd.controller;

import ru.virra.kkd.domain.comparison.IssueDelta;
import ru.virra.kkd.domain.comparison.MetricChange;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.openapi.model.IssueDeltaResponse;
import ru.virra.kkd.openapi.model.MetricCode;
import ru.virra.kkd.openapi.model.MetricTrend;
import ru.virra.kkd.openapi.model.MetricUnit;
import ru.virra.kkd.openapi.model.RunSnapshotResponse;
import ru.virra.kkd.openapi.model.SeverityResponse;
import ru.virra.kkd.openapi.model.SummaryMetricResponse;
import ru.virra.kkd.openapi.model.SummaryResponse;
import ru.virra.kkd.openapi.model.SummaryUnitResponse;
import ru.virra.kkd.readmodel.KkdSummary;
import ru.virra.kkd.readmodel.RunReference;
import ru.virra.kkd.readmodel.UnitReference;

import java.util.List;
import java.util.Locale;

/**
 * Формирует ответ сводной статистики из модели чтения {@link KkdSummary}.
 * Преобразует ссылки на прогоны, значения метрик, распределение проблем по уровням важности
 * и результаты сравнения в модели, созданные по контракту OpenAPI.
 * Преобразование идентификатора прогона в формат {@code RUN-001} выполняется только для сводки.
 */
final class SummaryResponseMapper {

    private SummaryResponseMapper() {
    }

    /**
     * Создаёт ответ API для выбранного прогона и подразделения.
     * При отсутствии прогона для сравнения поля {@code compared_to} и {@code delta} остаются {@code null}.
     *
     * @param summary сводная статистика, полученная из сервиса чтения
     * @return модель ответа, определённая контрактом OpenAPI
     */
    static SummaryResponse toResponse(KkdSummary summary) {
        RunSnapshotResponse run = toRunSnapshot(summary.run());
        RunSnapshotResponse comparedTo = toRunSnapshot(summary.comparedTo());

        UnitReference unit = summary.unit();
        SummaryUnitResponse unitResponse = new SummaryUnitResponse(unit.id(), unit.name());

        List<SummaryMetricResponse> metrics = summary.metrics().entrySet().stream()
                .map(entry -> toMetric(entry.getKey(), entry.getValue()))
                .toList();

        List<SeverityResponse> issuesBySeverity = summary.issuesBySeverity().stream()
                .map(change -> new SeverityResponse(
                        IssueSeverityMapper.toApi(change.severity()),
                        change.current(), change.previous(), change.delta()))
                .toList();

        IssueDelta delta = summary.delta();
        IssueDeltaResponse deltaResponse = delta == null ? null
                : new IssueDeltaResponse(delta.newCount(), delta.persisting(), delta.resolved());

        return new SummaryResponse(run, comparedTo, unitResponse, metrics,
                issuesBySeverity, deltaResponse);
    }

    /**
     * Преобразует ссылку на прогон для ответа сводной статистики.
     * Числовой идентификатор записывается с префиксом {@code RUN-} и минимум тремя цифрами.
     *
     * @param run ссылка на прогон или {@code null}, если прогона для сравнения нет
     * @return данные прогона для API или {@code null}
     */
    private static RunSnapshotResponse toRunSnapshot(RunReference run) {
        if (run == null) {
            return null;
        }
        String id = String.format(Locale.ROOT, "RUN-%03d", run.id());
        return new RunSnapshotResponse(id, run.snapshotDate());
    }

    /**
     * Формирует элемент списка метрик для ответа сводной статистики.
     *
     * @param key код и семантика метрики
     * @param change текущее значение и результат сравнения с предыдущим прогоном
     * @return метрика с названием, единицей измерения и направлением изменения
     */
    private static SummaryMetricResponse toMetric(MetricKey key, MetricChange change) {
        String title = switch (key) {
            case CLEAN_GOALS_SHARE -> "Доля корректных целей";
            case ISSUES_PER_GOAL -> "Среднее количество проблем на цель";
            case GOALS_WITH_OWNER_SHARE -> "Доля целей с ответственными";
            case GOALS_WITH_VALID_KR_WEIGHTS_SHARE -> "Доля целей с корректной суммой весов КР";
        };
        MetricCode code = MetricCode.fromValue(key.code());
        MetricUnit unit = key == MetricKey.ISSUES_PER_GOAL ? MetricUnit.COUNT : MetricUnit.PERCENT;
        MetricTrend trend = null;
        if (change.trend() != null) {
            trend = switch (change.trend()) {
                case BETTER -> MetricTrend.BETTER;
                case WORSE -> MetricTrend.WORSE;
                case SAME -> MetricTrend.SAME;
            };
        }

        return new SummaryMetricResponse(code, title, unit,
                change.value(), change.previous(), change.delta(), trend);
    }
}
