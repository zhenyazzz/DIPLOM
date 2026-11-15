package com.portfolio.identity.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.identity.api.identity.ChangeRoleRequest;
import com.portfolio.identity.api.identity.SetBlockedRequest;
import com.portfolio.identity.api.identity.UserResponse;
import com.portfolio.identity.api.identity.UserRole;
import com.portfolio.identity.auth.RefreshSessionService;
import com.portfolio.identity.shared.ApiException;
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
    RoleRepository roleRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    RefreshSessionService refreshSessionService;

    AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService =
                new AdminUserService(userRepository, roleRepository, userMapper, refreshSessionService);
    }

    @Test
    void changeRole_rejectsDemotingLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        UserEntity lastAdmin = userWithRole(adminId, "admin@example.com", "Admin", UserRole.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countActiveUsersWithRole(UserRole.ADMIN.name())).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.changeRole(
                        adminId, new ChangeRoleRequest(UserRole.INITIATOR)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(api.getCode()).isEqualTo("CONFLICT");
                });

        assertThat(lastAdmin.hasRole(UserRole.ADMIN)).isTrue();
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void setBlocked_rejectsBlockingLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        UserEntity lastAdmin = userWithRole(adminId, "admin@example.com", "Admin", UserRole.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countActiveUsersWithRole(UserRole.ADMIN.name())).thenReturn(1L);

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
        UserEntity initiator = userWithRole(userId, "user@example.com", "User", UserRole.INITIATOR);
        UserResponse expected =
                new UserResponse(userId, "user@example.com", "User", UserRole.INITIATOR, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(initiator));
        when(userMapper.toResponse(initiator)).thenReturn(expected);

        UserResponse result = adminUserService.setBlocked(userId, new SetBlockedRequest(true));

        assertThat(result.blocked()).isTrue();
        assertThat(initiator.isBlocked()).isTrue();
        verify(refreshSessionService).revokeAllForUser(userId);
        verify(userRepository, never()).countActiveUsersWithRole(any());
    }

    @Test
    void changeRole_allowsDemoteWhenAnotherActiveAdminExists() {
        UUID adminId = UUID.randomUUID();
        UserEntity admin = userWithRole(adminId, "admin@example.com", "Admin", UserRole.ADMIN);
        RoleEntity initiatorRole = role(UserRole.INITIATOR);
        UserResponse expected =
                new UserResponse(adminId, "admin@example.com", "Admin", UserRole.INITIATOR, false);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.countActiveUsersWithRole(UserRole.ADMIN.name())).thenReturn(2L);
        when(roleRepository.findByCode(UserRole.INITIATOR.name()))
                .thenReturn(Optional.of(initiatorRole));
        when(userMapper.toResponse(admin)).thenReturn(expected);

        UserResponse result = adminUserService.changeRole(
                adminId, new ChangeRoleRequest(UserRole.INITIATOR));

        assertThat(result.role()).isEqualTo(UserRole.INITIATOR);
        assertThat(admin.hasRole(UserRole.INITIATOR)).isTrue();
    }

    private static UserEntity userWithRole(UUID id, String email, String name, UserRole roleCode) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setEmail(email);
        entity.setDisplayName(name);
        entity.setRole(role(roleCode));
        entity.setBlocked(false);
        return entity;
    }

    private static RoleEntity role(UserRole code) {
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID());
        entity.setCode(code.name());
        entity.setName(code.name());
        return entity;
    }
}
