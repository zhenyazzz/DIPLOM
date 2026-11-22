package com.portfolio.gateway.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GraphqlOperationAccessTest {

    @Test
    void authenticatedReadsAllowedForEveryRole() {
        for (String role : new String[] {"INITIATOR", "EXPERT_ANALYST", "DECISION_MAKER", "ADMIN"}) {
            assertThat(GraphqlOperationAccess.isAllowed("project", role)).isTrue();
            assertThat(GraphqlOperationAccess.isAllowed("projects", role)).isTrue();
            assertThat(GraphqlOperationAccess.isAllowed("projectRegistryStats", role)).isTrue();
        }
    }

    @Test
    void initiatorMayRegisterAndSubmitOnly() {
        assertThat(GraphqlOperationAccess.isAllowed("registerInitiative", "INITIATOR")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("submitInitiative", "INITIATOR")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("startReview", "INITIATOR")).isFalse();
        assertThat(GraphqlOperationAccess.isAllowed("approveForPortfolio", "INITIATOR")).isFalse();
        assertThat(GraphqlOperationAccess.isAllowed("archiveInitiative", "INITIATOR")).isFalse();
    }

    @Test
    void expertMayReviewAndEvaluate() {
        assertThat(GraphqlOperationAccess.isAllowed("startReview", "EXPERT_ANALYST")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("completeEvaluation", "EXPERT_ANALYST")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("approveForPortfolio", "EXPERT_ANALYST")).isFalse();
    }

    @Test
    void decisionMakerMayApproveRejectReturn() {
        assertThat(GraphqlOperationAccess.isAllowed("approveForPortfolio", "DECISION_MAKER")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("rejectInitiative", "DECISION_MAKER")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("returnForRevision", "DECISION_MAKER")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("archiveInitiative", "DECISION_MAKER")).isFalse();
    }

    @Test
    void adminMayPerformEveryBusinessOperation() {
        assertThat(GraphqlOperationAccess.isAllowed("registerInitiative", "ADMIN")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("archiveInitiative", "ADMIN")).isTrue();
        assertThat(GraphqlOperationAccess.isAllowed("approveForPortfolio", "ADMIN")).isTrue();
    }
}
