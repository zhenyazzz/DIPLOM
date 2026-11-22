package com.portfolio.gateway.graphql;

import com.portfolio.common.security.jwt.AccessTokenClaims;
import com.portfolio.common.security.jwt.AccessTokenVerifier;
import com.portfolio.gateway.security.IdentitySessionClient;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.parser.Parser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * GraphQL gate: Bearer JWT → Auth Service Redis session → RBAC on root fields.
 * HTTP stays 200; failures are GraphQL errors with {@code extensions.code}.
 */
@Component
@RequiredArgsConstructor
public class GraphqlSecurityInterceptor implements WebGraphQlInterceptor {

    private final AccessTokenVerifier accessTokenVerifier;
    private final IdentitySessionClient identitySessionClient;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        List<String> rootFields = rootFields(request);
        boolean needsAuth = rootFields.stream().anyMatch(GraphqlOperationAccess::requiresAuthentication);
        if (!needsAuth) {
            return chain.next(request);
        }

        String rawToken = bearerToken(request);
        if (rawToken == null) {
            return Mono.just(errorResponse(request, "UNAUTHORIZED", "Authentication required"));
        }

        AccessTokenClaims claims;
        try {
            claims = accessTokenVerifier.verify(rawToken);
        } catch (IllegalArgumentException ex) {
            return Mono.just(errorResponse(request, "UNAUTHORIZED", "Invalid or expired access token"));
        }

        if (claims.jti() == null || claims.jti().isBlank()) {
            return Mono.just(errorResponse(request, "UNAUTHORIZED", "Access token has no jti"));
        }

        if (!identitySessionClient.isSessionActive(rawToken)) {
            return Mono.just(errorResponse(request, "UNAUTHORIZED", "Access session is not active"));
        }

        String role = claims.role();
        for (String field : rootFields) {
            if (!GraphqlOperationAccess.isAllowed(field, role)) {
                return Mono.just(errorResponse(
                        request,
                        "FORBIDDEN",
                        "Role " + role + " is not allowed to perform " + field));
            }
        }

        request.configureExecutionInput((executionInput, builder) -> builder
                .graphQLContext(Map.of(
                        AccessTokenClaims.class, claims,
                        "accessToken", rawToken))
                .build());
        return chain.next(request);
    }

    private static String bearerToken(WebGraphQlRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.length() < 8 || !value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = value.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static List<String> rootFields(WebGraphQlRequest request) {
        Document document = new Parser().parseDocument(request.getDocument());
        OperationDefinition operation = selectOperation(document, request.getOperationName());
        if (operation == null || operation.getSelectionSet() == null) {
            return List.of();
        }
        List<String> fields = new ArrayList<>();
        for (Selection<?> selection : operation.getSelectionSet().getSelections()) {
            if (selection instanceof Field field) {
                fields.add(field.getName());
            }
        }
        return fields;
    }

    private static OperationDefinition selectOperation(Document document, String operationName) {
        List<OperationDefinition> operations = document.getDefinitions().stream()
                .filter(OperationDefinition.class::isInstance)
                .map(OperationDefinition.class::cast)
                .toList();
        if (operations.isEmpty()) {
            return null;
        }
        if (operationName == null || operationName.isBlank()) {
            return operations.getFirst();
        }
        return operations.stream()
                .filter(op -> operationName.equals(op.getName()))
                .findFirst()
                .orElse(null);
    }

    private static WebGraphQlResponse errorResponse(WebGraphQlRequest request, String code, String message) {
        ErrorType errorType =
                "FORBIDDEN".equals(code) ? ErrorType.FORBIDDEN : ErrorType.UNAUTHORIZED;
        GraphQLError error = GraphqlErrorBuilder.newError()
                .errorType(errorType)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
        ExecutionResult result = ExecutionResultImpl.newExecutionResult()
                .addError(error)
                .build();
        ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput()
                .query(request.getDocument());
        if (request.getOperationName() != null) {
            inputBuilder.operationName(request.getOperationName());
        }
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(inputBuilder.build(), result));
    }
}
