# F04 — Password Reset

## Status
Complete — verified with SHA-256 token hashing at rest, 30-min expiry, anti-enumeration, global session invalidation, and 5 integration tests.

## Summary
Implemented secure password reset by email:
- `V6__password_reset_tokens.sql`: `password_reset_tokens` table with SHA-256 hashed tokens (`token_hash` unique index).
- `POST /api/auth/forgot-password`: Generates 32-byte secure random token, computes SHA-256 hash at rest, dispatches email, and always returns 200 OK without leaking account existence.
- `POST /api/auth/reset-password`: Validates token hash, enforces single-use and 30-minute expiry, updates password with Argon2id, and invalidates all active sessions in `SPRING_SESSION`.
- `EmailSender`: Pluggable email interface with `LoggingEmailSender` default.
- 5 comprehensive tests verifying happy path, single-use, expiry, anti-enumeration, and multi-device session revocation.

## Key Files
- `V6__password_reset_tokens.sql`: Token storage schema.
- `PasswordResetToken.java`: Token entity.
- `PasswordResetTokenRepository.java`: Token repository.
- `PasswordResetService.java`: Token lifecycle, hashing, and session invalidation logic.
- `EmailSender.java` & `LoggingEmailSender.java`: Transactional email delivery interface.
- `AuthController.java`: Exposes `/api/auth/forgot-password` and `/api/auth/reset-password`.
- `PasswordResetTest.java`: 5 automated tests.
