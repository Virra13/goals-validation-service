package ru.virra.kkd.controller;

import org.springframework.web.bind.annotation.RestController;
import ru.virra.kkd.openapi.api.RunsApi;
import ru.virra.kkd.openapi.model.RunListResponse;
import ru.virra.kkd.openapi.model.RunResponse;
import ru.virra.kkd.openapi.model.RunStatus;
import ru.virra.kkd.readmodel.RunListItem;
import ru.virra.kkd.service.KkdRunQueryService;

import java.util.List;

@RestController
public class KkdRunController implements RunsApi {

    private final KkdRunQueryService runs;

    public KkdRunController(KkdRunQueryService runs) {
        this.runs = runs;
    }

    @Override
    public RunListResponse listRuns() {
        List<RunResponse> content = runs.listRuns().stream()
                .map(KkdRunController::toResponse)
                .toList();

        return new RunListResponse(content);
    }

    private static RunResponse toResponse(RunListItem run) {
        RunStatus status = RunStatus.fromValue(run.status().name());
        return new RunResponse(run.id(), status, run.snapshotDate(),
                run.comparedToRunId(), run.issuesCount());
    }
}
