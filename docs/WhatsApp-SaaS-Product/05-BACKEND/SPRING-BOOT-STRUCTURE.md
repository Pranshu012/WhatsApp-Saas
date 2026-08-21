# Spring Boot Structure

See `03-ARCHITECTURE/APPLICATION-STRUCTURE.md` for the package tree and dependency rules, and
`MODULES.md` for per-module contracts.

This file covers the conventions *within* a module.

## Anatomy of a module

```text
whatsapp/
├── WhatsAppAccount.java              JPA entity — never leaves the module
├── WhatsAppAccountRepository.java    Spring Data, tenant-scoped
├── WhatsAppAccountService.java       business logic, @Transactional
├── WhatsAppController.java           REST, DTOs only
├── dto/
│   ├── ConnectRequest.java           record + bean validation
│   └── AccountResponse.java          record
├── signup/                           sub-feature
├── client/                           external integration
├── crypto/                           TokenCipher
└── webhook/                          sub-feature
```

## Conventions

**Constructor injection only.**
```java
@Service
public class WhatsAppAccountService {
    private final WhatsAppAccountRepository repository;
    private final TokenCipher cipher;

    public WhatsAppAccountService(WhatsAppAccountRepository repository, TokenCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }
}
```
No `@Autowired` fields — they hide dependencies, make the class untestable without Spring, and
allow circular dependencies to compile.

**`@Transactional` on services.** Not controllers (transaction spans serialisation) and not
repositories (too granular to express a business operation).

**Never expose entities.**
```java
// BAD
@GetMapping("/api/whatsapp/account")
public WhatsAppAccount get() { ... }        // leaks the encrypted token field, couples API to schema
```
```java
// GOOD
public record AccountResponse(String displayPhoneNumber, String verifiedName,
                              String qualityRating, boolean paymentMethodAttached,
                              Instant connectedAt) {}
```

**Records for DTOs.**
```java
public record ConnectRequest(
    @NotBlank String code,
    @NotBlank String wabaId,
    @NotBlank String phoneNumberId
) {}
```

**Exceptions.**
```java
public class DomainException extends RuntimeException {
    private final String code;          // stable, machine-readable
    private final HttpStatus status;
}

public class NotFoundException extends DomainException { ... }
public class ConflictException extends DomainException { ... }
```

`GlobalExceptionHandler` maps them to `ApiError` including the `requestId` from MDC, so a customer
can quote a code and you can find the exact log line.

**Typed configuration.**
```java
@ConfigurationProperties(prefix = "app.meta")
public record MetaProperties(String appId, String appSecret,
                             String graphVersion, String webhookVerifyToken) {}
```
Better than scattered `@Value` — one place to see what config exists, and it fails at startup
rather than at first use.

## Profiles

| Profile | Runs |
|---|---|
| `local` | Local dev: verbose logging, no-op email sender |
| `web` | HTTP API. No job polling. |
| `worker` | Job polling. Serves no HTTP traffic (though Actuator stays up for health checks). |

```java
@Component
@Profile("worker")
public class JobWorker { ... }
```

Production runs the **same JAR** twice, as two systemd units, with different
`SPRING_PROFILES_ACTIVE`. This is the mechanism that makes later horizontal scaling a config
change rather than a rewrite.

## Naming

| Thing | Pattern |
|---|---|
| Entity | `WhatsAppAccount` |
| Repository | `WhatsAppAccountRepository` |
| Service | `WhatsAppAccountService` |
| Controller | `WhatsAppController` |
| Request DTO | `ConnectRequest` |
| Response DTO | `AccountResponse` |
| Event | `WhatsAppAccountConnected` (past tense — it already happened) |
| Job handler | `SendWhatsAppMessageHandler` |
| Exception | `TokenExpiredException` |

## Test layout

```text
src/test/java/.../
├── unit/            JUnit + Mockito, no Spring context — fast
├── integration/     @SpringBootTest + Testcontainers Postgres
└── architecture/    ArchUnit / Spring Modulith boundary tests
```

```java
@SpringBootTest
@Testcontainers
abstract class IntegrationTest {
    @Container
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:17");
    // and connect as the NON-SUPERUSER app role, or RLS tests are meaningless
}
```

**Testcontainers, never H2.** We depend on `FOR UPDATE SKIP LOCKED`, Row-Level Security,
`pg_trgm`, `tsvector`, and `jsonb`. H2 emulates none of these faithfully; you'd get green tests
and a broken production.
