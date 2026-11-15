package com.portfolio.identity.identity.service;

import com.portfolio.common.web.config.CommonWebProperties;
import com.portfolio.common.web.filter.FieldAllowList;
import com.portfolio.common.web.filter.FieldRule;
import com.portfolio.common.web.filter.RsqlSpecificationBuilder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    static final FieldAllowList USER_FILTER_FIELDS = FieldAllowList.of(
            FieldRule.filterAndSort("email", String.class),
            FieldRule.filterAndSort("displayName", String.class),
            FieldRule.filterAndSort("blocked", boolean.class));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RefreshSessionService refreshSessionService;
    private final CommonWebProperties commonWebProperties;
    private final RsqlSpecificationBuilder rsqlBuilder = new RsqlSpecificationBuilder();

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable, String filter) {
        Pageable effective = clamp(pageable);
        Specification<UserEntity> spec = rsqlBuilder.build(filter, USER_FILTER_FIELDS);
        Page<UserResponse> result = userRepository.findAll(spec, effective).map(userMapper::toResponse);
        return PageResponse.from(result);
    }

    @Transactional
    public UserResponse changeRole(UUID userId, ChangeRoleRequest request) {
        UserEntity user = requireUser(userId);
        UserRole newRole = request.role();
        if (isActiveAdmin(user) && newRole != UserRole.ADMIN) {
            ensureNotLastActiveAdmin();
        }
        user.setRole(requireRoleEntity(newRole));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setBlocked(UUID userId, SetBlockedRequest request) {
        UserEntity user = requireUser(userId);
        boolean blocked = request.blocked();
        if (blocked && !user.isBlocked()) {
            if (user.hasRole(UserRole.ADMIN)) {
                ensureNotLastActiveAdmin();
            }
            user.setBlocked(true);
            refreshSessionService.revokeAllForUser(userId);
        } else if (!blocked) {
            user.setBlocked(false);
        }
        return userMapper.toResponse(user);
    }

    private RoleEntity requireRoleEntity(UserRole role) {
        return roleRepository
                .findByCode(role.name())
                .orElseThrow(() -> IdentityException.badRequest("Unknown role: " + role));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IdentityException.notFound("User not found"));
    }

    private void ensureNotLastActiveAdmin() {
        if (userRepository.countActiveUsersWithRole(UserRole.ADMIN.name()) <= 1) {
            throw IdentityException.conflict("Cannot demote or block the last active Admin");
        }
    }

    private static boolean isActiveAdmin(UserEntity user) {
        return user.hasRole(UserRole.ADMIN) && !user.isBlocked();
    }

    private Pageable clamp(Pageable pageable) {
        int max = commonWebProperties.getMaxPageSize();
        int defaultSize = commonWebProperties.getDefaultPageSize();

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize() <= 0 ? defaultSize : Math.min(pageable.getPageSize(), max);
        Sort sort = sanitizeSort(pageable.getSort());
        return PageRequest.of(page, size, sort);
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            USER_FILTER_FIELDS.requireSortable(order.getProperty());
            orders.add(order);
        }
        return Sort.by(orders);
    }
}
