package com.portfolio.common.web.crud;

public interface CrudOperations<ID, CREATE, UPDATE, RESPONSE> {

    RESPONSE create(CREATE request);

    RESPONSE get(ID id);

    RESPONSE update(ID id, UPDATE request);

    void delete(ID id);
}
