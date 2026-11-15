package com.portfolio.identity.identity.service;

import com.portfolio.common.security.context.SecuritySupport;
import com.portfolio.identity.api.identity.dto.UserResponse;
import com.portfolio.identity.error.IdentityException;
import com.portfolio.identity.identity.entity.UserEntity;
import com.portfolio.identity.identity.mapper.UserMapper;
import com.portfolio.identity.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecuritySupport security;

    @Transactional(readOnly = true)
    public UserResponse me() {
        UserEntity user = userRepository
                .findById(security.requireUserId())
                .orElseThrow(() -> IdentityException.unauthorized("User not found"));
        return userMapper.toResponse(user);
    }
}
