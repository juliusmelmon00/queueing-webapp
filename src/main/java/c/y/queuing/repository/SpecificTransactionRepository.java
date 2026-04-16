package c.y.queuing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.SpecificTransaction;

public interface SpecificTransactionRepository extends JpaRepository<SpecificTransaction, Long> {

    List<SpecificTransaction> findAllByOrderByDisplayOrderAsc();

    List<SpecificTransaction> findByTransactionTypeCodeAndIsActiveTrueOrderByDisplayOrderAsc(String transactionTypeCode);

    List<SpecificTransaction> findByTransactionTypeCodeOrderByDisplayOrderAsc(String transactionTypeCode);

    Optional<SpecificTransaction> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByTransactionTypeCodeAndCode(String transactionTypeCode, String code);

    boolean existsByTransactionTypeCodeAndNameIgnoreCase(String transactionTypeCode, String name);

    Optional<SpecificTransaction> findByTransactionTypeCodeAndCode(String transactionTypeCode, String code);

    Optional<SpecificTransaction> findByTransactionTypeCodeAndNameIgnoreCase(String transactionTypeCode, String name);
}