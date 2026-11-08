package ru.virra.kkd.controller;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestController;
import ru.virra.kkd.domain.model.IssueSeverity;
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

/**
 * Реализует операцию получения страницы проблем ККД по контракту {@link IssuesApi}.
 * Проверяет допустимые значения идентификаторов и параметров пагинации,
 * передаёт фильтры в {@link KkdIssueQueryService} и формирует модель ответа OpenAPI.
 * Фильтр по подразделению включает его дочерние подразделения на стороне сервиса.
 */
@RestController
public class KkdIssueController implements IssuesApi {

    private final KkdIssueQueryService issues;

    public KkdIssueController(KkdIssueQueryService issues) {
        this.issues = issues;
    }

    /**
     * Настраивает преобразование строкового параметра {@code severity} в важность проблемы API.
     *
     * @param binder механизм привязки параметра HTTP-запроса
     */
    @InitBinder("severity")
    void bindSeverity(WebDataBinder binder) {
        binder.registerCustomEditor(Severity.class, new SeverityQueryEditor());
    }

    /**
     * Настраивает проверку формата идентификаторов {@code run_id} и {@code unit_id}.
     *
     * @param binder механизм привязки параметров HTTP-запроса
     */
    @InitBinder({"run_id", "unit_id"})
    void bindIds(WebDataBinder binder) {
        binder.registerCustomEditor(Long.class, new StrictLongQueryEditor());
    }

    /**
     * Возвращает страницу проблем указанного прогона с необязательными фильтрами.
     * Общее число подходящих проблем определяется до применения пагинации.
     *
     * @param runId идентификатор прогона ККД
     * @param unitId идентификатор подразделения или {@code null} для всех подразделений
     * @param ruleCode код правила ККД или {@code null} для всех правил
     * @param severity важность проблемы или {@code null} без фильтра по важности
     * @param limit максимальное число проблем на странице, от 1 до 500
     * @param offset число подходящих проблем, пропускаемых перед страницей
     * @return модель страницы проблем, определённая контрактом OpenAPI
     * @throws InvalidKkdParameterException если идентификатор или параметры пагинации вне допустимого диапазона
     */
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

        IssueSeverity severityFilter = IssueSeverityMapper.toDomain(severity);
        IssuePage page = issues.findPage(runId, unitId, ruleCode, severityFilter, limit, offset);
        List<IssueResponse> content = page.content().stream()
                .map(KkdIssueController::toResponse)
                .toList();

        return new IssueListResponse(content, page.limit(), page.offset(), page.total());
    }

    private static IssueResponse toResponse(IssueListItem issue) {
        IssueObjectType objectType = switch (issue.objectType()) {
            case GOAL -> IssueObjectType.GOAL;
            case KEY_RESULT -> IssueObjectType.KEY_RESULT;
        };
        Severity severity = IssueSeverityMapper.toApi(issue.severity());
        return new IssueResponse(issue.id(), issue.ruleCode(), objectType,
                issue.objectId(), severity, issue.description());
    }
}
