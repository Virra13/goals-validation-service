package ru.virra.kkd.controller;

import org.springframework.web.bind.annotation.RestController;
import ru.virra.kkd.openapi.api.RunsApi;
import ru.virra.kkd.openapi.model.RunListResponse;
import ru.virra.kkd.openapi.model.RunResponse;
import ru.virra.kkd.openapi.model.RunStatus;
import ru.virra.kkd.readmodel.RunListItem;
import ru.virra.kkd.service.KkdRunQueryService;

import java.util.List;

/**
 * Реализует операцию получения списка прогонов ККД по контракту {@link RunsApi}.
 * Получает данные прогонов из {@link KkdRunQueryService} и преобразует их
 * в модели ответа OpenAPI, включая статус, дату снимка и число найденных проблем.
 */
@RestController
public class KkdRunController implements RunsApi {

    private final KkdRunQueryService runs;

    public KkdRunController(KkdRunQueryService runs) {
        this.runs = runs;
    }

    /**
     * Возвращает список прогонов в порядке от новых к старым.
     *
     * @return модель списка прогонов, определённая контрактом OpenAPI
     */
    @Override
    public RunListResponse listRuns() {
        List<RunResponse> content = runs.listRuns().stream()
                .map(KkdRunController::toResponse)
                .toList();

        return new RunListResponse(content);
    }

    private static RunResponse toResponse(RunListItem run) {
        RunStatus status = switch (run.status()) {
            case PENDING -> RunStatus.PENDING;
            case RUNNING -> RunStatus.RUNNING;
            case COMPLETED -> RunStatus.COMPLETED;
            case FAILED -> RunStatus.FAILED;
        };
        return new RunResponse(run.id(), status, run.snapshotDate(),
                run.comparedToRunId(), run.issuesCount());
    }
}
