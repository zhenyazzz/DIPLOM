package com.portfolio.gateway.catalog;

public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(ProjectStatus from, ProjectStatus to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
