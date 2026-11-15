package com.portfolio.identity.identity;

import com.portfolio.identity.api.identity.UserResponse;
import com.portfolio.identity.api.identity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role")
    UserResponse toResponse(UserEntity entity);

    default UserRole mapRole(RoleEntity role) {
        return role == null ? null : UserRole.valueOf(role.getCode());
    }
}
