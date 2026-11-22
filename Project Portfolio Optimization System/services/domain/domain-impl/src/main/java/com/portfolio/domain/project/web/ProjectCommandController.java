package com.portfolio.domain.project.web;

import com.portfolio.domain.api.project.ProjectCommandApi;
import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.RegisterProjectRequest;
import com.portfolio.domain.api.project.dto.StatusReasonRequest;
import com.portfolio.domain.project.ProjectLifecycleService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectCommandController implements ProjectCommandApi {

    private final ProjectLifecycleService lifecycle;

    @Override
    public ProjectCardResponse register(RegisterProjectRequest request) {
        return lifecycle.register(request);
    }

    @Override
    public ProjectCardResponse submit(UUID id) {
        return lifecycle.submit(id);
    }

    @Override
    public ProjectCardResponse startReview(UUID id) {
        return lifecycle.startReview(id);
    }

    @Override
    public ProjectCardResponse completeEvaluation(UUID id) {
        return lifecycle.completeEvaluation(id);
    }

    @Override
    public ProjectCardResponse approve(UUID id) {
        return lifecycle.approveForPortfolio(id);
    }

    @Override
    public ProjectCardResponse returnForRevision(UUID id, String reason, StatusReasonRequest body) {
        return lifecycle.returnForRevision(id, reasonOf(reason, body));
    }

    @Override
    public ProjectCardResponse reject(UUID id, String reason, StatusReasonRequest body) {
        return lifecycle.reject(id, reasonOf(reason, body));
    }

    @Override
    public ProjectCardResponse archive(UUID id) {
        return lifecycle.archive(id);
    }

    private static String reasonOf(String reason, StatusReasonRequest body) {
        if (body != null && body.reason() != null && !body.reason().isBlank()) {
            return body.reason();
        }
        return reason;
    }
}
