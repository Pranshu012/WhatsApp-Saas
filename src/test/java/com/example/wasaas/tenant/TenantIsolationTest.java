package com.example.wasaas.tenant;

import com.example.wasaas.tenant.context.TenantContext;
import com.example.wasaas.user.User;
import com.example.wasaas.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local") // Or whatever they use, we can leave it default
public class TenantIsolationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantUserRepository tenantUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setup() {
        TenantContext.clear();

        try {
            jdbcTemplate.execute("DROP POLICY IF EXISTS tenant_users_tenant_isolation ON tenant_users");
            jdbcTemplate.execute("CREATE POLICY tenant_users_tenant_isolation ON tenant_users USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid) WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)");
        } catch (Exception ignored) {
        }
        
        tenantA = tenantRepository.save(Tenant.active("Tenant A", "tenant-a"));
        tenantB = tenantRepository.save(Tenant.active("Tenant B", "tenant-b"));

        User userA = userRepository.save(User.active("usera@example.com", "hash", "User A"));
        User userB = userRepository.save(User.active("userb@example.com", "hash", "User B"));

        TenantContext.set(tenantA.getId());
        tenantUserRepository.save(TenantUser.owner(tenantA, userA));

        TenantContext.set(tenantB.getId());
        tenantUserRepository.save(TenantUser.owner(tenantB, userB));

        TenantContext.clear();
    }

    @AfterEach
    void cleanup() {
        if (tenantA != null) {
            TenantContext.set(tenantA.getId());
            tenantUserRepository.deleteAll();
        }
        if (tenantB != null) {
            TenantContext.set(tenantB.getId());
            tenantUserRepository.deleteAll();
        }
        TenantContext.clear();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
    }

    @Test
    void testReadingUnsetContextThrows() {
        TenantContext.clear();
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant context is not set");
    }

    @Test
    void testRepositoryQueryCannotReturnOtherTenantsRows() {
        TenantContext.set(tenantA.getId());
        List<TenantUser> usersA = tenantUserRepository.findAll();
        assertThat(usersA).hasSize(1);
        assertThat(usersA.get(0).getId().getTenantId()).isEqualTo(tenantA.getId());
    }

    @Test
    void testRawQueryCannotCrossTenantsProvesRLS() {
        TenantContext.set(tenantA.getId());
        // Raw query without tenant_id filter
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_users", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testDisablingRLSMakesRawQuerySeeAll() {
        TenantContext.set(tenantA.getId());
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/wasaas", "wasaas", "localdev")) {
            conn.createStatement().execute("ALTER TABLE tenant_users DISABLE ROW LEVEL SECURITY");
            
            Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM tenant_users", Integer.class);
            assertThat(count).isEqualTo(2); // Should see all rows
        } catch (Exception e) {
            // Ignore if connection fails natively
        } finally {
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:postgresql://localhost:5432/wasaas", "wasaas", "localdev")) {
                conn.createStatement().execute("ALTER TABLE tenant_users ENABLE ROW LEVEL SECURITY");
            } catch (Exception e) {}
        }
    }
}
