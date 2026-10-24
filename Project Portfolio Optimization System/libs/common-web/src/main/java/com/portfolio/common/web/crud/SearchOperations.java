package com.portfolio.common.web.crud;

import com.portfolio.common.web.web.PageResponse;
import org.springframework.data.domain.Pageable;

public interface SearchOperations<RESPONSE> {

    PageResponse<RESPONSE> search(Pageable pageable, String filter);
}
