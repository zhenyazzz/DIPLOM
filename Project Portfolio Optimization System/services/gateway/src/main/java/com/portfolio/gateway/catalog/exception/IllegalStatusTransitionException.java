package com.portfolio.gateway.catalog.exception;

import com.portfolio.gateway.catalog.model.ProjectStatus;

public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(ProjectStatus from, ProjectStatus to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
