package com.portfolio.gateway.graphql;

import java.util.Map;
import java.util.Set;

/**
 * Server-side RBAC for GraphQL root fields. Roles match identity {@code roles.code}.
 */
public final class GraphqlOperationAccess {

    private static final Set<String> ANY_AUTHENTICATED =
            Set.of("INITIATOR", "EXPERT_ANALYST", "DECISION_MAKER", "ADMIN");
    private static final Set<String> INITIATOR_OR_ADMIN = Set.of("INITIATOR", "ADMIN");
    private static final Set<String> EXPERT_OR_ADMIN = Set.of("EXPERT_ANALYST", "ADMIN");
    private static final Set<String> DECISION_OR_ADMIN = Set.of("DECISION_MAKER", "ADMIN");
    private static final Set<String> ADMIN_ONLY = Set.of("ADMIN");

    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("project", ANY_AUTHENTICATED),
            Map.entry("projects", ANY_AUTHENTICATED),
            Map.entry("projectRegistryStats", ANY_AUTHENTICATED),
            Map.entry("registerInitiative", INITIATOR_OR_ADMIN),
            Map.entry("submitInitiative", INITIATOR_OR_ADMIN),
            Map.entry("startReview", EXPERT_OR_ADMIN),
            Map.entry("completeEvaluation", EXPERT_OR_ADMIN),
            Map.entry("approveForPortfolio", DECISION_OR_ADMIN),
            Map.entry("returnForRevision", DECISION_OR_ADMIN),
            Map.entry("rejectInitiative", DECISION_OR_ADMIN),
            Map.entry("archiveInitiative", ADMIN_ONLY));

    private GraphqlOperationAccess() {}

    public static boolean isIntrospection(String fieldName) {
        return fieldName != null && fieldName.startsWith("__");
    }

    public static boolean isAllowed(String fieldName, String role) {
        if (isIntrospection(fieldName)) {
            return true;
        }
        if (role == null || role.isBlank()) {
            return false;
        }
        Set<String> roles = ALLOWED.get(fieldName);
        return roles != null && roles.contains(role);
    }

    public static boolean requiresAuthentication(String fieldName) {
        return !isIntrospection(fieldName);
    }
}
