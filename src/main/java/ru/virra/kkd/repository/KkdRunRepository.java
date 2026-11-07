package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.virra.kkd.domain.model.RunStatus;
import ru.virra.kkd.entity.KkdRun;
import ru.virra.kkd.repository.projection.RunListRow;

import java.util.List;

public interface KkdRunRepository extends JpaRepository<KkdRun, Long> {
    List<KkdRun> findAllByOrderBySnapshotDateDesc();
    List<KkdRun> findAllByStatusOrderBySnapshotDateDesc(RunStatus status);

    @Query("""
            select new ru.virra.kkd.repository.projection.RunListRow(
                r.id, r.status, r.snapshotDate, base.id, count(i.id)
            )
            from KkdRun r
            left join r.comparedToRun base
            left join KkdIssue i on i.run = r
            group by r.id, r.status, r.snapshotDate, base.id
            order by r.snapshotDate desc, r.id desc
            """)
    List<RunListRow> findRunList();
}
