package com.portfolio.common.web.crud;

import java.util.UUID;

public interface ResourceOperations<CREATE, UPDATE, RESPONSE>
        extends CrudOperations<UUID, CREATE, UPDATE, RESPONSE>, SearchOperations<RESPONSE> {
}
