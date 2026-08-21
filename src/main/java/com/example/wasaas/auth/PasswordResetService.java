package com.example.wasaas.auth;

import com.example.wasaas.common.email.EmailSender;
import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import com.example.wasaas.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final long EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailSender emailSender,
                                JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().getStatus() == UserStatus.ACTIVE) {
            User user = userOpt.get();
            tokenRepository.deleteByUserId(user.getId());

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);
            Instant expiresAt = Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

            tokenRepository.save(new PasswordResetToken(user.getId(), tokenHash, expiresAt));

            String subject = "Password Reset Request";
            String body = "You requested to reset your password. Use the following link to set a new password: "
                    + "https://app.wasaas.com/reset-password?token=" + rawToken;
            emailSender.send(user.getEmail(), subject, body);
        } else {
            // Timing defense against user enumeration
            hashToken("dummy-timing-token-" + email);
        }

        return Map.of("message", "If that email is registered, password reset instructions have been sent.");
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token().trim());
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token");
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isUsed() || token.isExpired()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token"));

        // Update password with Argon2 hash
        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark token as used
        token.markUsed();
        tokenRepository.save(token);

        // Invalidate all active sessions for this user in Spring Session JDBC
        jdbcTemplate.update("DELETE FROM spring_session WHERE principal_name = ?", user.getEmail());

        return Map.of("message", "Password has been successfully reset. Please log in with your new password.");
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
