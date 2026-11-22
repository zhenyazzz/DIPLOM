package com.portfolio.domain.api.project;

import com.portfolio.domain.api.project.dto.ProjectCardResponse;
import com.portfolio.domain.api.project.dto.RegisterProjectRequest;
import com.portfolio.domain.api.project.dto.StatusReasonRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Write contract for initiative lifecycle ({@code /api/v1/projects}).
 * Mutations are business transitions, not generic CRUD.
 */
@RequestMapping("/api/v1/projects")
public interface ProjectCommandApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProjectCardResponse register(@Valid @RequestBody RegisterProjectRequest request);

    @PostMapping("/{id}/submit")
    ProjectCardResponse submit(@PathVariable UUID id);

    @PostMapping("/{id}/start-review")
    ProjectCardResponse startReview(@PathVariable UUID id);

    @PostMapping("/{id}/complete-evaluation")
    ProjectCardResponse completeEvaluation(@PathVariable UUID id);

    @PostMapping("/{id}/approve")
    ProjectCardResponse approve(@PathVariable UUID id);

    @PostMapping("/{id}/return")
    ProjectCardResponse returnForRevision(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) StatusReasonRequest body);

    @PostMapping("/{id}/reject")
    ProjectCardResponse reject(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) StatusReasonRequest body);

    @PostMapping("/{id}/archive")
    ProjectCardResponse archive(@PathVariable UUID id);
}
