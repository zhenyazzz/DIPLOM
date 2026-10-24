package com.portfolio.common.web.crud;

import com.portfolio.common.web.config.CommonWebProperties;
import com.portfolio.common.web.domain.BaseEntity;
import com.portfolio.common.web.filter.FieldAllowList;
import com.portfolio.common.web.filter.RsqlSpecificationBuilder;
import com.portfolio.common.web.web.NotFoundException;
import com.portfolio.common.web.web.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class AbstractJpaCrudService<E extends BaseEntity, CREATE, UPDATE, RESPONSE>
        implements ResourceOperations<CREATE, UPDATE, RESPONSE> {

    private final BaseRepository<E> repository;
    private final EntityMapper<E, CREATE, UPDATE, RESPONSE> mapper;
    private final FieldAllowList allowList;
    private final RsqlSpecificationBuilder rsqlBuilder;
    private final CommonWebProperties properties;

    protected AbstractJpaCrudService(
            BaseRepository<E> repository,
            EntityMapper<E, CREATE, UPDATE, RESPONSE> mapper,
            FieldAllowList allowList,
            CommonWebProperties properties) {
        this(repository, mapper, allowList, properties, new RsqlSpecificationBuilder());
    }

    protected AbstractJpaCrudService(
            BaseRepository<E> repository,
            EntityMapper<E, CREATE, UPDATE, RESPONSE> mapper,
            FieldAllowList allowList,
            CommonWebProperties properties,
            RsqlSpecificationBuilder rsqlBuilder) {
        this.repository = repository;
        this.mapper = mapper;
        this.allowList = allowList;
        this.properties = properties;
        this.rsqlBuilder = rsqlBuilder;
    }

    protected BaseRepository<E> repository() {
        return repository;
    }

    protected EntityMapper<E, CREATE, UPDATE, RESPONSE> mapper() {
        return mapper;
    }

    protected FieldAllowList allowList() {
        return allowList;
    }

    @Override
    public RESPONSE create(CREATE request) {
        E entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public RESPONSE get(UUID id) {
        return mapper.toResponse(requireEntity(id));
    }

    @Override
    public RESPONSE update(UUID id, UPDATE request) {
        E entity = requireEntity(id);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RESPONSE> search(Pageable pageable, String filter) {
        Pageable effective = clamp(pageable);
        Specification<E> spec = rsqlBuilder.build(filter, allowList);
        Page<RESPONSE> result = repository.findAll(spec, effective).map(mapper::toResponse);
        return PageResponse.from(result);
    }

    protected E requireEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    protected NotFoundException notFound(UUID id) {
        return new NotFoundException("Resource not found: " + id);
    }

    protected Pageable clamp(Pageable pageable) {
        int max = properties.getMaxPageSize();
        int defaultSize = properties.getDefaultPageSize();

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? defaultSize : Math.min(pageable.getPageSize(), max);
        Sort sort = sanitizeSort(pageable.getSort());
        return PageRequest.of(page, size, sort);
    }

    protected Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            allowList.requireSortable(order.getProperty());
            orders.add(order);
        }
        return Sort.by(orders);
    }
}
