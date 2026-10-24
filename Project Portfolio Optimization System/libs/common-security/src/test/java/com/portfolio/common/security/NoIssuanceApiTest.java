package com.portfolio.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.portfolio.common.security.audit.JwtAuditorAware;
import com.portfolio.common.security.autoconfigure.PortfolioSecurityAutoConfiguration;
import com.portfolio.common.security.autoconfigure.PortfolioSecurityProperties;
import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.common.security.jwt.AccessTokenClaims;
import com.portfolio.common.security.jwt.AccessTokenVerifier;
import com.portfolio.common.security.web.ProblemDetailsAccessDeniedHandler;
import com.portfolio.common.security.web.ProblemDetailsAuthEntryPoint;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class NoIssuanceApiTest {

    private static final List<Class<?>> PUBLIC_TYPES = List.of(
            PortfolioSecurityAutoConfiguration.class,
            PortfolioSecurityProperties.class,
            AccessTokenVerifier.class,
            AccessTokenClaims.class,
            SecuritySupport.class,
            JwtAuditorAware.class,
            ProblemDetailsAuthEntryPoint.class,
            ProblemDetailsAccessDeniedHandler.class);

    @Test
    void publicApiHasNoIssueOrSignMethods() {
        for (Class<?> type : PUBLIC_TYPES) {
            Stream<Method> forbidden = Arrays.stream(type.getMethods())
                    .filter(m -> m.getDeclaringClass() == type)
                    .filter(m -> isIssuanceMethod(m.getName()));
            assertThat(forbidden)
                    .as("type %s must not expose issue/sign APIs", type.getName())
                    .isEmpty();
        }
    }

    private static boolean isIssuanceMethod(String methodName) {
        String name = methodName.toLowerCase(Locale.ROOT);
        return name.equals("issue")
                || name.startsWith("issueaccess")
                || name.startsWith("issuetoken")
                || name.equals("sign")
                || name.startsWith("signjwt")
                || name.startsWith("signtoken")
                || name.equals("mint")
                || name.startsWith("minttoken");
    }
}
