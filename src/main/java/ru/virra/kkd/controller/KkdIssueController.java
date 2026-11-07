package ru.virra.kkd.controller;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestController;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.openapi.api.IssuesApi;
import ru.virra.kkd.openapi.model.IssueListResponse;
import ru.virra.kkd.openapi.model.IssueObjectType;
import ru.virra.kkd.openapi.model.IssueResponse;
import ru.virra.kkd.openapi.model.Severity;
import ru.virra.kkd.readmodel.IssueListItem;
import ru.virra.kkd.readmodel.IssuePage;
import ru.virra.kkd.service.KkdIssueQueryService;

import java.util.List;

@RestController
public class KkdIssueController implements IssuesApi {

    private final KkdIssueQueryService issues;

    public KkdIssueController(KkdIssueQueryService issues) {
        this.issues = issues;
    }

    @InitBinder("severity")
    void bindSeverity(WebDataBinder binder) {
        binder.registerCustomEditor(Severity.class, new SeverityQueryEditor());
    }

    @InitBinder({"run_id", "unit_id"})
    void bindIds(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new StrictLongQueryEditor());
    }

    @Override
    public IssueListResponse listIssues(Long runId, Long unitId, String ruleCode,
                                        Severity severity, Integer limit, Integer offset) {
        if (runId <= 0 || (unitId != null && unitId <= 0)) {
            throw new InvalidKkdParameterException("ID должны быть больше нуля");
        }
        if (limit <= 0 || limit > 500 || offset < 0) {
            throw new InvalidKkdParameterException(
                    "limit должен быть от 1 до 500, offset — не меньше нуля");
        }

        ru.virra.kkd.domain.model.Severity severityFilter = severity == null
                ? null : ru.virra.kkd.domain.model.Severity.valueOf(severity.name());
        IssuePage page = issues.findPage(runId, unitId, ruleCode, severityFilter, limit, offset);
        List<IssueResponse> content = page.content().stream()
                .map(KkdIssueController::toResponse)
                .toList();

        return new IssueListResponse(content, page.limit(), page.offset(), page.total());
    }

    private static IssueResponse toResponse(IssueListItem issue) {
        IssueObjectType objectType = IssueObjectType.fromValue(issue.objectType().name());
        Severity severity = Severity.valueOf(issue.severity().name());
        return new IssueResponse(issue.id(), issue.ruleCode(), objectType,
                issue.objectId(), severity, issue.description());
    }
}
