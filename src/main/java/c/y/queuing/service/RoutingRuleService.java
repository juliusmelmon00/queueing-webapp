package c.y.queuing.service;

import java.util.List;

import org.springframework.stereotype.Service;

import c.y.queuing.entity.RoutingRule;
import c.y.queuing.repository.RoutingRuleRepository;

@Service
public class RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;

    public RoutingRuleService(RoutingRuleRepository routingRuleRepository) {
        this.routingRuleRepository = routingRuleRepository;
    }

    // OLD METHOD (KEEP FOR COMPATIBILITY)
    public String resolveDepartment(String transactionTypeCode) {
        return resolveDepartment(transactionTypeCode, null);
    }

    // ✅ UPDATED METHOD (NO HARDCODED DEFAULT)
    public String resolveDepartment(String transactionTypeCode, String specificTransactionCode) {

        if (transactionTypeCode == null || transactionTypeCode.isBlank()) {
            throw new RuntimeException("Transaction type is required for routing.");
        }

        String txCode = transactionTypeCode.trim().toUpperCase();

        String specificCode = (specificTransactionCode == null || specificTransactionCode.isBlank())
                ? null
                : specificTransactionCode.trim().toUpperCase();

        // ✅ STEP 1: EXACT MATCH
        if (specificCode != null) {
            List<RoutingRule> exactRules =
                    routingRuleRepository.findByTransactionTypeCodeAndSpecificTransactionCodeAndIsActiveTrueOrderByPriorityAsc(
                            txCode, specificCode);

            if (!exactRules.isEmpty()) {
                return exactRules.get(0).getTargetDepartmentCode().trim().toUpperCase();
            }
        }

        // ✅ STEP 2: FALLBACK (transaction only)
        List<RoutingRule> fallbackRules =
                routingRuleRepository.findByTransactionTypeCodeAndSpecificTransactionCodeIsNullAndIsActiveTrueOrderByPriorityAsc(
                        txCode);

        if (!fallbackRules.isEmpty()) {
            return fallbackRules.get(0).getTargetDepartmentCode().trim().toUpperCase();
        }

        // ❌ NO DEFAULT → THROW ERROR
        throw new RuntimeException(
                "No routing rule found for transaction: " + txCode +
                (specificCode != null ? (" and specific: " + specificCode) : "")
        );
    }
}