package com.portfolio.gateway.graphql;

import com.portfolio.gateway.catalog.InvalidProjectIdException;
import com.portfolio.gateway.catalog.ProjectNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class PortfolioGraphqlErrors {

    @GraphQlExceptionHandler
    public GraphQLError notFound(ProjectNotFoundException ex) {
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of("code", "NOT_FOUND"))
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError badId(InvalidProjectIdException ex) {
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("code", "BAD_REQUEST"))
                .build();
    }
}
