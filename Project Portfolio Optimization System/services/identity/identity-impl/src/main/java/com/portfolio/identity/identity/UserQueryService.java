package com.portfolio.identity.identity;

import com.portfolio.identity.api.identity.UserResponse;
import com.portfolio.identity.shared.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse me() {
        UUID userId = currentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return userMapper.toResponse(user);
    }

    private static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return UUID.fromString(jwt.getSubject());
    }
}
