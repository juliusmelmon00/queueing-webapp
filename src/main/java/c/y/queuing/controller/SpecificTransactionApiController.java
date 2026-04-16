package c.y.queuing.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import c.y.queuing.entity.SpecificTransaction;
import c.y.queuing.entity.TransactionType;
import c.y.queuing.repository.SpecificTransactionRepository;
import c.y.queuing.repository.TransactionTypeRepository;

@RestController
@RequestMapping("/admin/config/specific-transactions")
public class SpecificTransactionApiController {

    private final SpecificTransactionRepository specificTransactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    public SpecificTransactionApiController(
            SpecificTransactionRepository specificTransactionRepository,
            TransactionTypeRepository transactionTypeRepository) {
        this.specificTransactionRepository = specificTransactionRepository;
        this.transactionTypeRepository = transactionTypeRepository;
    }

    @GetMapping("/by-transaction-type/{id}")
    public List<SpecificTransactionOption> getByTransactionType(@PathVariable Long id) {
        TransactionType transactionType = transactionTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid transaction type id: " + id));

        List<SpecificTransaction> items =
                specificTransactionRepository.findByTransactionTypeCodeAndIsActiveTrueOrderByDisplayOrderAsc(
                        transactionType.getCode()
                );

        return items.stream()
                .map(s -> new SpecificTransactionOption(
                        s.getCode(),
                        s.getName(),
                        s.getTransactionTypeCode()
                ))
                .collect(Collectors.toList());
    }

    public static class SpecificTransactionOption {
        private final String code;
        private final String name;
        private final String transactionTypeCode;

        public SpecificTransactionOption(String code, String name, String transactionTypeCode) {
            this.code = code;
            this.name = name;
            this.transactionTypeCode = transactionTypeCode;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getTransactionTypeCode() {
            return transactionTypeCode;
        }
    }
}