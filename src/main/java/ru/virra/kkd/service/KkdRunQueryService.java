package ru.virra.kkd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.virra.kkd.readmodel.RunListItem;
import ru.virra.kkd.repository.KkdRunRepository;

import java.time.ZoneOffset;
import java.util.List;

@Service
public class KkdRunQueryService {

    private final KkdRunRepository runs;

    public KkdRunQueryService(KkdRunRepository runs) {
        this.runs = runs;
    }

    @Transactional(readOnly = true)
    public List<RunListItem> listRuns() {
        return runs.findRunList().stream()
                .map(row -> new RunListItem(
                        row.id(),
                        row.status(),
                        row.snapshotDate().atOffset(ZoneOffset.UTC).toLocalDate(),
                        row.comparedToRunId(),
                        row.issuesCount()))
                .toList();
    }
}
