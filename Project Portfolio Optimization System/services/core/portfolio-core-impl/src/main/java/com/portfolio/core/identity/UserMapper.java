package com.portfolio.core.identity;

import com.portfolio.core.api.identity.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(UserEntity entity);
}
