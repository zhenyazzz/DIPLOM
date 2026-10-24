package com.portfolio.common.web.crud;

public interface EntityMapper<E, CREATE, UPDATE, RESPONSE> {

    E toEntity(CREATE createRequest);

    void updateEntity(UPDATE updateRequest, E entity);

    RESPONSE toResponse(E entity);
}
