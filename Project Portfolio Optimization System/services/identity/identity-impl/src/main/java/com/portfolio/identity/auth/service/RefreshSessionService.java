package com.portfolio.identity.auth.service;

import com.portfolio.identity.auth.RefreshSessionEntity;
import com.portfolio.identity.auth.RefreshSessionRepository;
import com.portfolio.identity.config.AuthProperties;
import com.portfolio.identity.identity.entity.UserEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshSessionRepository refreshSessionRepository;
    private final AuthProperties authProperties;

    @Transactional
    public String create(UserEntity user) {
        String raw = generateRawToken();
        RefreshSessionEntity session = newSession(user, raw);
        refreshSessionRepository.save(session);
        return raw;
    }

    @Transactional
    public Optional<RotationResult> rotate(String rawToken) {
        Optional<RefreshSessionEntity> existing = resolveActive(rawToken);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        RefreshSessionEntity old = existing.get();
        UserEntity user = old.getUser();
        if (user.isBlocked()) {
            old.setRevokedAt(Instant.now());
            return Optional.empty();
        }

        String newRaw = generateRawToken();
        RefreshSessionEntity replacement = newSession(user, newRaw);
        refreshSessionRepository.save(replacement);

        Instant now = Instant.now();
        old.setRevokedAt(now);
        old.setReplacedBy(replacement);
        return Optional.of(new RotationResult(newRaw, user));
    }

    @Transactional
    public void revokeOne(String rawToken) {
        refreshSessionRepository.findByTokenHash(hash(rawToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshSessionRepository.revokeAllActiveByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshSessionEntity> resolveActive(String rawToken) {
        Instant now = Instant.now();
        return refreshSessionRepository.findByTokenHash(hash(rawToken))
                .filter(s -> s.isActive(now));
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private RefreshSessionEntity newSession(UserEntity user, String raw) {
        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setUser(user);
        session.setTokenHash(hash(raw));
        session.setExpiresAt(Instant.now().plus(authProperties.getJwt().getRefreshTokenTtl()));
        return session;
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(String rawRefreshToken, UserEntity user) {
    }
}
