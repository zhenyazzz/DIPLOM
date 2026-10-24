package com.portfolio.common.web.web;

import com.portfolio.common.web.filter.FilterBadRequestException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonWebProblemDetailsHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(FilterBadRequestException.class)
    public ProblemDetail badFilter(FilterBadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Filter", "BAD_FILTER", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code);
        return pd;
    }
}
