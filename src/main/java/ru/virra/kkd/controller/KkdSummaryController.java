package ru.virra.kkd.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.openapi.api.SummaryApi;
import ru.virra.kkd.openapi.model.SummaryResponse;
import ru.virra.kkd.readmodel.KkdSummary;
import ru.virra.kkd.service.KkdSummaryService;

@RestController
public class KkdSummaryController implements SummaryApi {

    private final KkdSummaryService summaries;

    public KkdSummaryController(KkdSummaryService summaries) {
        this.summaries = summaries;
    }

    @InitBinder({"run_id", "unit_id"})
    void bindIds(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new StrictLongQueryEditor());
    }

    @Override
    public SummaryResponse getSummary(Long runId, Long unitId) {
        if (runId <= 0 || unitId <= 0) {
            throw new InvalidKkdParameterException(
                    "run_id и unit_id должны быть больше нуля");
        }

        KkdSummary summary = summaries.getSummary(runId, unitId);
        return SummaryResponseMapper.toResponse(summary);
    }
}
