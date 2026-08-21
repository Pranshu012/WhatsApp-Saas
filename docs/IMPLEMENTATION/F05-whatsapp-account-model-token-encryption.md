# F05 — WhatsApp Account Model and Token Encryption

## Status
Complete — verified with AES-256-GCM envelope encryption, random 12-byte IVs, startup fail-fast validation, leak-proof entity design, and multi-tenant RLS isolation across 34 automated tests.

## Summary
Implemented secure storage for customer WhatsApp accounts without Meta API calls:
- `V7__whatsapp_accounts.sql`: `whatsapp_accounts` table storing WABA IDs, phone number IDs, quality rating, messaging limit tiers, and `access_token_encrypted BYTEA`. Protected by PostgreSQL Row-Level Security policy (`whatsapp_accounts_tenant_isolation`).
- `TokenCipher`: AES-256-GCM cipher generating a fresh 12-byte random IV/nonce per encryption, combined with ciphertext and 128-bit authentication tag.
- Fail-fast key validation: startup immediately aborts with `IllegalStateException` if `TOKEN_ENCRYPTION_KEY` is missing or not exactly 32 bytes (256 bits).
- Leak-proof entity design: `WhatsAppAccount` exposes no getter for plaintext access tokens; Jackson JSON serialization and logs cannot leak tokens. Decryption is available exclusively via explicit internal method `WhatsAppAccountService.getDecryptedToken(accountId)`.
- Global `@FilterDef` registration in `package-info.java` to prevent duplicate filter definition conflicts across entities in Hibernate 6.

## Key Files
- `V7__whatsapp_accounts.sql`: Database migration with RLS.
- `TokenCipher.java`: AES-256-GCM encryption/decryption cipher.
- `WhatsAppAccount.java`: JPA entity extending `BaseTenantEntity`.
- `WhatsAppAccountRepository.java`: Repository with WABA/Phone query methods.
- `WhatsAppAccountService.java`: Account lifecycle and safe token decryption service.
- `TokenCipherTest.java`: 7 cryptographic unit tests.
- `WhatsAppAccountTest.java`: 4 integration tests verifying encryption at rest, Jackson safety, and multi-tenant isolation.
