package com.portfolio.identity.auth.service;

import com.portfolio.identity.api.auth.dto.LoginRequest;
import com.portfolio.identity.api.auth.dto.RegisterRequest;
import com.portfolio.identity.api.auth.dto.TokenResponse;
import com.portfolio.identity.api.identity.dto.UserResponse;
import com.portfolio.identity.api.identity.UserRole;
import com.portfolio.identity.identity.entity.RoleEntity;
import com.portfolio.identity.identity.repository.RoleRepository;
import com.portfolio.identity.identity.entity.UserEntity;
import com.portfolio.identity.identity.mapper.UserMapper;
import com.portfolio.identity.identity.repository.UserRepository;
import com.portfolio.identity.error.IdentityException;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionService refreshSessionService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw IdentityException.conflict("Email already registered");
        }
        RoleEntity initiator = roleRepository
                .findByCode(UserRole.INITIATOR.name())
                .orElseThrow(() -> IdentityException.conflict("Role INITIATOR is not seeded"));
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(initiator);
        user.setBlocked(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<UserEntity> found = userRepository.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            throw IdentityException.unauthorized(INVALID_CREDENTIALS);
        }
        UserEntity user = found.get();
        if (user.isBlocked() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw IdentityException.unauthorized(INVALID_CREDENTIALS);
        }
        String rawRefresh = refreshSessionService.create(user);
        return toAuthResult(user, rawRefresh);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw IdentityException.unauthorized("Missing refresh token");
        }
        RefreshSessionService.RotationResult rotation = refreshSessionService.rotate(rawRefreshToken)
                .orElseThrow(() -> IdentityException.unauthorized("Invalid refresh token"));
        return toAuthResult(rotation.user(), rotation.rawRefreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshSessionService.revokeOne(rawRefreshToken);
        }
    }

    private AuthResult toAuthResult(UserEntity user, String rawRefresh) {
        String access = jwtTokenService.issueAccessToken(user);
        TokenResponse tokens = new TokenResponse(access, jwtTokenService.accessExpiresInSeconds());
        return new AuthResult(tokens, rawRefresh);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthResult(TokenResponse tokens, String rawRefreshToken) {
    }
}
