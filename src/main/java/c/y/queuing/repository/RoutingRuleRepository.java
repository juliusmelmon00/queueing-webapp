package c.y.queuing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.RoutingRule;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {

    List<RoutingRule> findAllByOrderByTransactionTypeCodeAscPriorityAsc();

    boolean existsByTargetDepartmentCode(String code);

    boolean existsByTransactionTypeCode(String code);

    boolean existsByTransactionTypeCodeAndTargetDepartmentCode(
            String transactionTypeCode,
            String targetDepartmentCode);

    boolean existsByTransactionTypeCodeAndSpecificTransactionCodeAndTargetDepartmentCode(
            String transactionTypeCode,
            String specificTransactionCode,
            String targetDepartmentCode);

    boolean existsByTransactionTypeCodeAndSpecificTransactionCodeIsNullAndTargetDepartmentCode(
            String transactionTypeCode,
            String targetDepartmentCode);

    boolean existsBySpecificTransactionCode(String specificTransactionCode);

    List<RoutingRule> findByTransactionTypeCodeAndSpecificTransactionCodeAndIsActiveTrueOrderByPriorityAsc(
            String transactionTypeCode,
            String specificTransactionCode);

    List<RoutingRule> findByTransactionTypeCodeAndSpecificTransactionCodeIsNullAndIsActiveTrueOrderByPriorityAsc(
            String transactionTypeCode);
    
    //========FOR CLONING ROUTING RULES ===================
    List<RoutingRule> findByTransactionTypeCodeOrderByPriorityAsc(String transactionTypeCode);
    
}