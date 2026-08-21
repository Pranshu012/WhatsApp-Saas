package com.example.wasaas.automation.faq;

import com.example.wasaas.common.exception.DomainException;
import com.example.wasaas.tenant.context.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqRepository faqRepository;
    private final FaqMatchService faqMatchService;

    public FaqController(FaqRepository faqRepository, FaqMatchService faqMatchService) {
        this.faqRepository = faqRepository;
        this.faqMatchService = faqMatchService;
    }

    @GetMapping
    public ResponseEntity<List<Faq>> listFaqs() {
        UUID tenantId = TenantContext.require();
        return ResponseEntity.ok(faqRepository.findAllByTenantId(tenantId));
    }

    @PostMapping
    public ResponseEntity<Faq> createFaq(@Valid @RequestBody CreateFaqDto dto) {
        Faq faq = faqMatchService.createFaq(dto.question(), dto.answer());
        return ResponseEntity.status(HttpStatus.CREATED).body(faq);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaq(@PathVariable UUID id) {
        UUID tenantId = TenantContext.require();
        Faq faq = faqRepository.findById(id)
                .filter(f -> f.getTenantId().equals(tenantId))
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "FAQ not found: " + id));
        faqRepository.delete(faq);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<FaqMatchResult> testMatch(@RequestBody Map<String, String> request) {
        UUID tenantId = TenantContext.require();
        String question = request.get("question");
        FaqMatchResult result = faqMatchService.findMatch(tenantId, question);
        return ResponseEntity.ok(result);
    }

    public record CreateFaqDto(
            @NotBlank String question,
            @NotBlank String answer
    ) {}
}
