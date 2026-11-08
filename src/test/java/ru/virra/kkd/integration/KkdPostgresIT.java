package ru.virra.kkd.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.virra.kkd.domain.metric.MetricKey;
import ru.virra.kkd.exception.InvalidKkdParameterException;
import ru.virra.kkd.service.KkdRunExecutionService;
import ru.virra.kkd.service.KkdSummaryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import({KkdPostgresIT.FixedTime.class, SqlRecordingConfiguration.class})
class KkdPostgresIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @TestConfiguration
    static class FixedTime {
        @Bean
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired KkdRunExecutionService execution;
    @Autowired KkdSummaryService summaries;
    @Autowired JdbcTemplate jdbc;
    @Autowired SqlRecordingConfiguration.JdbcCalls calls;
    @Autowired MockMvc mvc;

    @Test
    void persistsCompleteRunAndReadsItsMetricsThroughProjection() {
        long id = execution.execute(2L);
        try {
            assertEquals("COMPLETED", jdbc.queryForObject("select status from kkd_run where id=?", String.class, id));
            assertEquals(24L, count("select count(*) from kkd_run_metric where run_id=?", id));
            assertEquals(10L, count("select count(*) from kkd_issue where run_id=?", id));
            var summary = summaries.getSummary(id, 1);
            assertEquals(new BigDecimal("50.0000"), summary.metrics().get(MetricKey.CLEAN_GOALS_SHARE).value());
            assertEquals(0, summary.delta().newCount());
            assertEquals(10, summary.delta().persisting());
            assertEquals(0, summary.delta().resolved());
        } finally {
            deleteRun(id);
        }
    }

    @Test
    void queryCountDoesNotGrowWithNumberOfUnits() {
        calls.reset();
        long firstRun = execution.execute(null);
        long baseline = calls.dataSelects();
        List<Long> extraUnits = new ArrayList<>();
        Long secondRun = null;
        try {
            for (int i = 0; i < 100; i++) {
                extraUnits.add(jdbc.queryForObject(
                        "insert into unit(name,parent_id) values ('integration-unit',1) returning id", Long.class));
            }
            calls.reset();
            secondRun = execution.execute(null);
            assertTrue(baseline > 0, "The recorder must observe real SELECTs");
            assertEquals(baseline, calls.dataSelects(), "Data SELECT count must not grow with units");
            assertEquals(424L, count("select count(*) from kkd_run_metric where run_id=?", secondRun));
            assertEquals(400L, count("select count(*) from kkd_run_metric where run_id=? and value is null", secondRun));
        } finally {
            if (secondRun != null) deleteRun(secondRun);
            deleteRun(firstRun);
            extraUnits.forEach(id -> jdbc.update("delete from unit where id=?", id));
        }
    }

    @Test
    void batchesMoreThanOneChunkOfIssues() {
        List<Long> extraGoals = new ArrayList<>();
        Long runId = null;
        try {
            for (int i = 0; i < 300; i++) {
                extraGoals.add(jdbc.queryForObject("""
                        insert into goal(unit_id,title,status) values (1,'integration-goal','ACTIVE') returning id
                        """, Long.class));
            }
            calls.reset();
            runId = execution.execute(null);
            assertEquals(610, calls.issueBatches().stream().mapToInt(Integer::intValue).sum());
            assertEquals(24, calls.metricBatches().stream().mapToInt(Integer::intValue).sum());
            assertTrue(calls.issueBatches().contains(50), "Hibernate must execute full batches");
            assertTrue(calls.issueBatches().stream().allMatch(size -> size > 0 && size <= 50));
            assertEquals(0, calls.unbatchedResultInserts(), "Result INSERTs must use executeBatch");
            assertEquals(610L, count("select count(*) from kkd_issue where run_id=?", runId));
        } finally {
            if (runId != null) deleteRun(runId);
            extraGoals.forEach(id -> jdbc.update("delete from goal where id=?", id));
        }
    }

    @Test
    void rollsBackRunAndBatchedIssuesWhenMetricInsertFails() {
        long runs = count("select count(*) from kkd_run");
        long issues = count("select count(*) from kkd_issue");
        long metrics = count("select count(*) from kkd_run_metric");
        jdbc.execute("alter table kkd_run_metric add constraint integration_reject_new_runs check (run_id <= 2)");
        try {
            assertThrows(DataAccessException.class, () -> execution.execute(2L));
            assertEquals(runs, count("select count(*) from kkd_run"));
            assertEquals(issues, count("select count(*) from kkd_issue"));
            assertEquals(metrics, count("select count(*) from kkd_run_metric"));
        } finally {
            jdbc.execute("alter table kkd_run_metric drop constraint integration_reject_new_runs");
        }
    }

    @Test
    void rejectsUnfinishedComparisonRun() {
        long id = jdbc.queryForObject("""
                insert into kkd_run(status,snapshot_date) values ('RUNNING',now()) returning id
                """, Long.class);
        try {
            assertThrows(InvalidKkdParameterException.class, () -> execution.execute(id));
        } finally {
            deleteRun(id);
        }
    }

    @Test
    void apiPreservesFirstRunNullsAndDatabaseFilters() throws Exception {
        mvc.perform(get("/api/kkd/summary?run_id=1&unit_id=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"compared_to\":null")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"delta\":null")))
                .andExpect(jsonPath("$.metrics.length()").value(4));
        mvc.perform(get("/api/kkd/issues?run_id=2&unit_id=2&rule_code=GOAL_NO_KR&severity=critical"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.content[0].object_id").value(14));
        mvc.perform(get("/api/kkd/issues?run_id=2&offset=5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.content.length()").value(5));
        mvc.perform(get("/api/kkd/issues?run_id=2&limit=3&offset=2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.offset").value(2))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").value(10));
        mvc.perform(get("/api/kkd/issues?run_id=2&limit=3&offset=100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/kkd/summary?run_id=999999&unit_id=1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/kkd/issues?run_id=2&unit_id=999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/kkd/issues?run_id=2&rule_code=UNKNOWN"))
                .andExpect(status().isUnprocessableEntity());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private void deleteRun(long id) {
        jdbc.update("delete from kkd_run where id=?", id);
    }
}
