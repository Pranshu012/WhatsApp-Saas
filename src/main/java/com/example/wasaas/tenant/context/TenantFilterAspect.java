package com.example.wasaas.tenant.context;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableTenantFilter() {
        Session session = entityManager.unwrap(Session.class);
        if (TenantContext.get() != null) {
            session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.get());
        } else {
            session.disableFilter("tenantFilter");
        }
    }
}
