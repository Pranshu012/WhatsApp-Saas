package com.example.wasaas.automation.faq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findAllByTenantId(UUID tenantId);

    @Query(value = """
            SELECT
                f.id AS id,
                f.tenant_id AS tenantId,
                f.question AS question,
                f.answer AS answer,
                f.enabled AS enabled,
                similarity(f.question, :query) AS trgmScore,
                ts_rank(f.search_vector, plainto_tsquery('english', :query)) AS tsScore,
                (
                    0.6 * similarity(f.question, :query) +
                    0.4 * LEAST(1.0, ts_rank(f.search_vector, plainto_tsquery('english', :query)) * 2.0)
                ) AS combinedScore
            FROM faqs f
            WHERE f.tenant_id = :tenantId
              AND f.enabled = true
              AND (
                  similarity(f.question, :query) > 0.15
                  OR f.search_vector @@ plainto_tsquery('english', :query)
              )
            ORDER BY combinedScore DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<FaqMatchProjection> findBestMatch(@Param("tenantId") UUID tenantId, @Param("query") String query);
}
