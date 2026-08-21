package com.example.wasaas.dashboard;

import com.example.wasaas.ledger.BillingCategory;
import com.example.wasaas.ledger.BillingCategoryCount;
import com.example.wasaas.ledger.MessageLedgerRepository;
import com.example.wasaas.ledger.MessageLedgerStatus;
import com.example.wasaas.ledger.StatusOutcomeCount;
import com.example.wasaas.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MessageLedgerRepository ledgerRepository;

    public DashboardController(MessageLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public record DashboardStatsResponse(
            String currentMonth,
            long totalMessages,
            Map<String, Long> categoryCounts,
            Map<String, Long> deliveryOutcomes,
            double deliveryRatePercent,
            String note
    ) {}

    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        UUID tenantId = TenantContext.require();

        // Calculate current month date boundaries
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        YearMonth currentYearMonth = YearMonth.now(zone);
        ZonedDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime endOfMonth = currentYearMonth.plusMonths(1).atDay(1).atStartOfDay(zone);

        Instant start = startOfMonth.toInstant();
        Instant end = endOfMonth.toInstant();

        List<BillingCategoryCount> categoryCountsList = ledgerRepository.countByCategoryForDateRange(tenantId, start, end);
        List<StatusOutcomeCount> statusCountsList = ledgerRepository.countByStatusForDateRange(tenantId, start, end);

        Map<String, Long> categoryCounts = new HashMap<>();
        for (BillingCategory cat : BillingCategory.values()) {
            categoryCounts.put(cat.name(), 0L);
        }
        long total = 0;
        for (BillingCategoryCount item : categoryCountsList) {
            if (item.getCategory() != null) {
                categoryCounts.put(item.getCategory().name(), item.getTotal());
                total += item.getTotal();
            }
        }

        Map<String, Long> deliveryOutcomes = new HashMap<>();
        for (MessageLedgerStatus st : MessageLedgerStatus.values()) {
            deliveryOutcomes.put(st.name(), 0L);
        }
        for (StatusOutcomeCount item : statusCountsList) {
            if (item.getStatus() != null) {
                deliveryOutcomes.put(item.getStatus().name(), item.getTotal());
            }
        }

        long sent = deliveryOutcomes.getOrDefault("SENT", 0L);
        long delivered = deliveryOutcomes.getOrDefault("DELIVERED", 0L);
        long read = deliveryOutcomes.getOrDefault("READ", 0L);
        long failed = deliveryOutcomes.getOrDefault("FAILED", 0L);
        long totalOutbound = sent + delivered + read + failed;

        double deliveryRate = totalOutbound > 0
                ? ((double) (delivered + read) / totalOutbound) * 100.0
                : 100.0;

        return new DashboardStatsResponse(
                currentYearMonth.getMonth().name() + " " + currentYearMonth.getYear(),
                total,
                categoryCounts,
                deliveryOutcomes,
                Math.round(deliveryRate * 10.0) / 10.0,
                "These are our platform ledger counts for your reference. Meta's official invoice in WhatsApp Manager is authoritative."
        );
    }
}
