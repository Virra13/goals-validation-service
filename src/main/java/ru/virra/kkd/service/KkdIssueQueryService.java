package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.exception.KkdResourceNotFoundException;
import ru.virra.kkd.readmodel.IssuePage;
import ru.virra.kkd.repository.KkdIssueSearchRepository;
import ru.virra.kkd.repository.KkdRuleRepository;
import ru.virra.kkd.repository.KkdRunRepository;
import ru.virra.kkd.repository.UnitRepository;

import java.util.List;

/**
 * Координирует чтение страницы проблем ККД.
 * Проверяет существование прогона и подразделения, известность кода правила,
 * определяет поддерево подразделения и передаёт фильтры в репозиторий поиска.
 */
@Service
public class KkdIssueQueryService {

    private final KkdRunRepository runs;
    private final UnitRepository units;
    private final KkdRuleRepository rules;
    private final KkdIssueSearchRepository issueSearch;

    public KkdIssueQueryService(KkdRunRepository runs, UnitRepository units,
                                KkdRuleRepository rules, KkdIssueSearchRepository issueSearch) {
        this.runs = runs;
        this.units = units;
        this.rules = rules;
        this.issueSearch = issueSearch;
    }

    /**
     * Получает страницу проблем прогона с необязательными фильтрами.
     * Если указано подразделение, выборка включает все его дочерние подразделения.
     * Отсутствующий необязательный фильтр передаётся как {@code null}.
     *
     * @param runId идентификатор прогона ККД
     * @param unitId идентификатор подразделения или {@code null} для всех подразделений
     * @param ruleCode код правила ККД или {@code null} для всех правил
     * @param severity важность проблемы или {@code null} без фильтра по важности
     * @param limit максимальное число проблем на странице
     * @param offset число подходящих проблем, пропускаемых перед страницей
     * @return страница проблем и общее число записей до применения пагинации
     * @throws KkdResourceNotFoundException если прогон или подразделение не найдены
     * @throws InvalidKkdParameterException если код правила ККД неизвестен
     */
    @Transactional(readOnly = true)
    public IssuePage findPage(long runId, Long unitId, String ruleCode,
                              IssueSeverity severity, int limit, int offset) {
        if (!runs.existsById(runId)) {
            throw new KkdResourceNotFoundException("Прогон " + runId + " не найден");
        }

        List<Long> unitIds = null;
        if (unitId != null) {
            unitIds = units.findSubtreeIds(unitId);
            if (unitIds.isEmpty()) {
                throw new KkdResourceNotFoundException("Подразделение " + unitId + " не найдено");
            }
        }

        if (ruleCode != null && rules.findByCode(ruleCode).isEmpty()) {
            throw new InvalidKkdParameterException("Неизвестный код правила " + ruleCode);
        }

        return issueSearch.findPage(runId, unitIds, ruleCode, severity, limit, offset);
    }
}
