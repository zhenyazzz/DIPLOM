package com.portfolio.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.core.api.identity.ChangeRoleRequest;
import com.portfolio.core.api.identity.SetBlockedRequest;
import com.portfolio.core.api.identity.UserResponse;
import com.portfolio.core.api.identity.UserRole;
import com.portfolio.core.auth.RefreshSessionService;
import com.portfolio.core.shared.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    RefreshSessionService refreshSessionService;

    AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, userMapper, refreshSessionService);
    }

    @Test
    void changeRole_rejectsDemotingLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        UserEntity lastAdmin = admin(adminId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRoleAndBlockedFalse(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.changeRole(
                        adminId, new ChangeRoleRequest(UserRole.INITIATOR)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("CONFLICT");
                });

        assertThat(lastAdmin.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void setBlocked_rejectsBlockingLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        UserEntity lastAdmin = admin(adminId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRoleAndBlockedFalse(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.setBlocked(adminId, new SetBlockedRequest(true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("CONFLICT");
                });

        assertThat(lastAdmin.isBlocked()).isFalse();
        verify(refreshSessionService, never()).revokeAllForUser(any());
    }

    @Test
    void setBlocked_blocksNonAdminAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity initiator = new UserEntity();
        initiator.setId(userId);
        initiator.setEmail("user@example.com");
        initiator.setDisplayName("User");
        initiator.setRole(UserRole.INITIATOR);
        initiator.setBlocked(false);

        UserResponse expected = new UserResponse(
                userId, "user@example.com", "User", UserRole.INITIATOR, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(initiator));
        when(userMapper.toResponse(initiator)).thenReturn(expected);

        UserResponse result = adminUserService.setBlocked(userId, new SetBlockedRequest(true));

        assertThat(result.blocked()).isTrue();
        assertThat(initiator.isBlocked()).isTrue();
        verify(refreshSessionService).revokeAllForUser(userId);
        verify(userRepository, never()).countByRoleAndBlockedFalse(any());
    }

    @Test
    void changeRole_allowsDemoteWhenAnotherActiveAdminExists() {
        UUID adminId = UUID.randomUUID();
        UserEntity admin = admin(adminId);
        UserResponse expected = new UserResponse(
                adminId, "admin@example.com", "Admin", UserRole.INITIATOR, false);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndBlockedFalse(UserRole.ADMIN)).thenReturn(2L);
        when(userMapper.toResponse(admin)).thenReturn(expected);

        UserResponse result = adminUserService.changeRole(
                adminId, new ChangeRoleRequest(UserRole.INITIATOR));

        assertThat(result.role()).isEqualTo(UserRole.INITIATOR);
        assertThat(admin.getRole()).isEqualTo(UserRole.INITIATOR);
    }

    private static UserEntity admin(UUID id) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setEmail("admin@example.com");
        entity.setDisplayName("Admin");
        entity.setRole(UserRole.ADMIN);
        entity.setBlocked(false);
        return entity;
    }
}
