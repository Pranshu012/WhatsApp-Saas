package com.example.wasaas.automation.faq;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class FaqMatchService {

    private static final Logger log = LoggerFactory.getLogger(FaqMatchService.class);

    private final FaqRepository faqRepository;
    private double confidenceThreshold;

    public FaqMatchService(FaqRepository faqRepository,
                           @Value("${app.automation.faq.confidence-threshold:0.35}") double confidenceThreshold) {
        this.faqRepository = faqRepository;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Transactional(readOnly = true)
    public FaqMatchResult findMatch(UUID tenantId, String query) {
        if (tenantId == null || query == null || query.isBlank()) {
            return FaqMatchResult.noMatch();
        }

        Optional<FaqMatchProjection> matchOpt = faqRepository.findBestMatch(tenantId, query.trim());

        if (matchOpt.isEmpty()) {
            log.debug("No FAQ candidate found for query [{}] under tenant [{}]", query, tenantId);
            return FaqMatchResult.noMatch();
        }

        FaqMatchProjection proj = matchOpt.get();
        double score = proj.getCombinedScore() != null ? proj.getCombinedScore() : 0.0;
        boolean confident = score >= confidenceThreshold;

        log.info("FAQ candidate [{}] matched query [{}] with score [{}] (threshold={}, confident={})",
                proj.getQuestion(), query, score, confidenceThreshold, confident);

        return new FaqMatchResult(
                proj.getId(),
                proj.getQuestion(),
                proj.getAnswer(),
                score,
                confident
        );
    }

    @Transactional
    public Faq createFaq(String question, String answer) {
        UUID tenantId = TenantContext.require();

        if (question == null || question.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "FAQ question cannot be empty");
        }
        if (answer == null || answer.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "FAQ answer cannot be empty");
        }

        Faq faq = new Faq(tenantId, question.trim(), answer.trim(), true);
        return faqRepository.save(faq);
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
}
