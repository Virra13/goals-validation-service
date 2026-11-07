package ru.virra.kkd.readmodel;

import java.util.List;

public record IssuePage(List<IssueListItem> content, int limit, int offset, long total) {
}
