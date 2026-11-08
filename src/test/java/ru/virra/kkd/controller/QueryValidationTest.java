package ru.virra.kkd.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.virra.kkd.domain.model.IssueObjectType;
import ru.virra.kkd.domain.model.IssueSeverity;
import ru.virra.kkd.readmodel.IssueListItem;
import ru.virra.kkd.readmodel.IssuePage;
import ru.virra.kkd.service.KkdIssueQueryService;
import ru.virra.kkd.service.KkdSummaryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QueryValidationTest {
    private MockMvc mvc;
    private KkdIssueQueryService issues;

    @BeforeEach
    void setup() {
        issues = mock(KkdIssueQueryService.class);
        when(issues.findPage(anyLong(), nullable(Long.class), nullable(String.class),
                nullable(IssueSeverity.class), anyInt(), anyInt()))
                .thenAnswer(call -> new IssuePage(List.of(), call.getArgument(4), call.getArgument(5), 0));
        mvc = MockMvcBuilders.standaloneSetup(new KkdIssueController(issues),
                        new KkdSummaryController(mock(KkdSummaryService.class)))
                .setControllerAdvice(new KkdApiExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"unit_id=", "unit_id=%20", "unit_id=abc", "unit_id=0", "unit_id=-1",
            "unit_id=1&unit_id=2", "limit=0", "limit=501", "limit=2147483647", "offset=-1",
            "severity=", "severity=CRITICAL", "severity=minor&severity=critical"})
    void rejectsInvalidFiltersWithoutCallingService(String query) throws Exception {
        mvc.perform(get("/api/kkd/issues?run_id=1&" + query))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
        verifyNoInteractions(issues);
    }

    @Test
    void allowsOmittedUnitAndMaximumPageSize() throws Exception {
        mvc.perform(get("/api/kkd/issues?run_id=1&limit=500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.limit").value(500));
        verify(issues).findPage(1, null, null, null, 500, 0);
    }

    @ParameterizedTest
    @CsvSource({"critical, CRITICAL", "major, MAJOR", "minor, MINOR"})
    void convertsSeverityBetweenApiAndDomain(String queryValue, IssueSeverity domainSeverity) throws Exception {
        IssueListItem issue = new IssueListItem(7, "KR_STALE_PROGRESS",
                IssueObjectType.KEY_RESULT, 12, domainSeverity, "Test issue");
        when(issues.findPage(1, null, null, domainSeverity, 5, 0))
                .thenReturn(new IssuePage(List.of(issue), 5, 0, 1));

        mvc.perform(get("/api/kkd/issues?run_id=1&severity=" + queryValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].severity").value(queryValue))
                .andExpect(jsonPath("$.content[0].object_type").value("KEY_RESULT"));
        verify(issues).findPage(1, null, null, domainSeverity, 5, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "run_id=", "run_id=abc", "run_id=0", "run_id=1&run_id=2"})
    void rejectsMissingOrInvalidRunId(String query) throws Exception {
        mvc.perform(get("/api/kkd/issues?" + query)).andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(issues);
    }

    @Test
    void summaryAlsoRejectsEmptyUnit() throws Exception {
        mvc.perform(get("/api/kkd/summary?run_id=1&unit_id="))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }
}
