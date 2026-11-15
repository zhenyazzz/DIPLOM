package com.portfolio.identity.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.common.web.config.CommonWebProperties;
import com.portfolio.common.web.filter.FilterBadRequestException;
import com.portfolio.common.web.web.PageResponse;
import com.portfolio.identity.api.identity.UserRole;
import com.portfolio.identity.api.identity.dto.ChangeRoleRequest;
import com.portfolio.identity.api.identity.dto.SetBlockedRequest;
import com.portfolio.identity.api.identity.dto.UserResponse;
import com.portfolio.identity.auth.service.RefreshSessionService;
import com.portfolio.identity.error.IdentityException;
import com.portfolio.identity.identity.entity.RoleEntity;
import com.portfolio.identity.identity.entity.UserEntity;
import com.portfolio.identity.identity.mapper.UserMapper;
import com.portfolio.identity.identity.repository.RoleRepository;
import com.portfolio.identity.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        adminUserService = new AdminUserService(
                userRepository,
                roleRepository,
                userMapper,
                refreshSessionService,
                new CommonWebProperties());
    }

    @Test
    void listUsers_returnsPageResponse() {
        UUID userId = UUID.randomUUID();
        UserEntity user = userWithRole(userId, "user@example.com", "User", UserRole.INITIATOR);
        UserResponse response =
                new UserResponse(userId, "user@example.com", "User", UserRole.INITIATOR, false);
        Pageable pageable = PageRequest.of(0, 20);
        Page<UserEntity> page = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(response);

        PageResponse<UserResponse> result = adminUserService.listUsers(pageable, null);

        assertThat(result.content()).containsExactly(response);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void userFilterAllowList_rejectsSensitiveAndUnknownFields() {
        assertThatThrownBy(() -> AdminUserService.USER_FILTER_FIELDS.requireFilterable("passwordHash"))
                .isInstanceOf(FilterBadRequestException.class);
        assertThatThrownBy(() -> AdminUserService.USER_FILTER_FIELDS.requireFilterable("role"))
                .isInstanceOf(FilterBadRequestException.class);
        assertThat(AdminUserService.USER_FILTER_FIELDS.requireFilterable("email").type())
                .isEqualTo(String.class);
        assertThat(AdminUserService.USER_FILTER_FIELDS.requireFilterable("blocked").type())
                .isEqualTo(boolean.class);
    }

    @Test
    void listUsers_rejectsUnknownSortField() {
        assertThatThrownBy(() -> adminUserService.listUsers(
                        PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("passwordHash")),
                        null))
                .isInstanceOf(FilterBadRequestException.class);

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void changeRole_rejectsDemotingLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        UserEntity lastAdmin = userWithRole(adminId, "admin@example.com", "Admin", UserRole.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countActiveUsersWithRole(UserRole.ADMIN.name())).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.changeRole(
                        adminId, new ChangeRoleRequest(UserRole.INITIATOR)))
                .isInstanceOf(IdentityException.class)
                .satisfies(ex -> {
                    IdentityException api = (IdentityException) ex;
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
                .isInstanceOf(IdentityException.class)
                .satisfies(ex -> {
                    IdentityException api = (IdentityException) ex;
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
