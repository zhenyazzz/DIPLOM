package com.portfolio.core.identity;

import com.portfolio.core.api.identity.ChangeRoleRequest;
import com.portfolio.core.api.identity.SetBlockedRequest;
import com.portfolio.core.api.identity.UserResponse;
import com.portfolio.core.api.identity.UserRole;
import com.portfolio.core.auth.RefreshSessionService;
import com.portfolio.core.shared.ApiException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshSessionService refreshSessionService;

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(UUID userId, ChangeRoleRequest request) {
        UserEntity user = requireUser(userId);
        UserRole newRole = request.role();
        if (isActiveAdmin(user) && newRole != UserRole.ADMIN) {
            ensureNotLastActiveAdmin();
        }
        user.setRole(newRole);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setBlocked(UUID userId, SetBlockedRequest request) {
        UserEntity user = requireUser(userId);
        boolean blocked = request.blocked();
        if (blocked && !user.isBlocked()) {
            if (user.getRole() == UserRole.ADMIN) {
                ensureNotLastActiveAdmin();
            }
            user.setBlocked(true);
            refreshSessionService.revokeAllForUser(userId);
        } else if (!blocked) {
            user.setBlocked(false);
        }
        return userMapper.toResponse(user);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private void ensureNotLastActiveAdmin() {
        if (userRepository.countByRoleAndBlockedFalse(UserRole.ADMIN) <= 1) {
            throw ApiException.conflict("Cannot demote or block the last active Admin");
        }
    }

    private static boolean isActiveAdmin(UserEntity user) {
        return user.getRole() == UserRole.ADMIN && !user.isBlocked();
    }
}
