package com.portfolio.domain.project;

import java.util.Set;

public final class ProjectStatuses {

    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String EVALUATED = "EVALUATED";
    public static final String APPROVED_FOR_PORTFOLIO = "APPROVED_FOR_PORTFOLIO";
    public static final String REJECTED = "REJECTED";
    public static final String RETURNED_FOR_REVISION = "RETURNED_FOR_REVISION";
    public static final String ARCHIVED = "ARCHIVED";

    public static final Set<String> ALL = Set.of(
            DRAFT,
            SUBMITTED,
            UNDER_REVIEW,
            EVALUATED,
            APPROVED_FOR_PORTFOLIO,
            REJECTED,
            RETURNED_FOR_REVISION,
            ARCHIVED);

    public static final Set<String> SORT_FIELDS = Set.of("CODE", "TITLE", "REQUESTED_BUDGET", "STATUS");

    private ProjectStatuses() {}
}
