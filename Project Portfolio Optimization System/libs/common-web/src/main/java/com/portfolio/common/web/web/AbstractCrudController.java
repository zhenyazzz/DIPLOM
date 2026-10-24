package com.portfolio.common.web.web;

import com.portfolio.common.web.crud.ResourceOperations;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class AbstractCrudController<CREATE, UPDATE, RESPONSE> {

    private final ResourceOperations<CREATE, UPDATE, RESPONSE> operations;

    protected AbstractCrudController(ResourceOperations<CREATE, UPDATE, RESPONSE> operations) {
        this.operations = operations;
    }

    protected ResourceOperations<CREATE, UPDATE, RESPONSE> operations() {
        return operations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RESPONSE create(@Valid @RequestBody CREATE request) {
        return operations.create(request);
    }

    @GetMapping("/{id}")
    public RESPONSE get(@PathVariable("id") UUID id) {
        return operations.get(id);
    }

    @PutMapping("/{id}")
    public RESPONSE update(@PathVariable("id") UUID id, @Valid @RequestBody UPDATE request) {
        return operations.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        operations.delete(id);
    }

    @GetMapping
    public PageResponse<RESPONSE> list(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(name = "filter", required = false) String filter) {
        return operations.search(pageable, filter);
    }
}
