package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.domain.model.Severity;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.exception.KkdResourceNotFoundException;
import ru.virra.kkd.readmodel.IssuePage;
import ru.virra.kkd.repository.KkdIssueSearchRepository;
import ru.virra.kkd.repository.KkdRuleRepository;
import ru.virra.kkd.repository.KkdRunRepository;
import ru.virra.kkd.repository.UnitRepository;

import java.util.List;

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

    @Transactional(readOnly = true)
    public IssuePage findPage(long runId, Long unitId, String ruleCode,
                              Severity severity, int limit, int offset) {
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
