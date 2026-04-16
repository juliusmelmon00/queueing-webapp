package c.y.queuing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "routing_rule")
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Example: OFF_SIGNER, SUBMIT_DOCS
    @Column(name = "transaction_type_code", nullable = false)
    private String transactionTypeCode;
    
    // for specific transaction code for 2nd dropdown
    @Column(name = "specific_transaction_code")
    private String specificTransactionCode;;

    // Optional operator (can be null)
    @Column(name = "operator_number")
    private Integer operatorNumber;

    // Target department code (RECRUITMENT, OPERATOR_1, etc.)
    @Column(name = "target_department_code", nullable = false)
    private String targetDepartmentCode;

    // Priority order (lower number = higher priority)
    @Column(nullable = false)
    private Integer priority = 1;

    // Enable/disable rule
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // =========================
    // GETTERS AND SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public String getTransactionTypeCode() {
        return transactionTypeCode;
    }

    public void setTransactionTypeCode(String transactionTypeCode) {
        this.transactionTypeCode = transactionTypeCode;
    }
    
    public String getSpecificTransactionCode() {
    	return specificTransactionCode;
    }
    
    public void setSpecificTransactionCode(String specificTransactionCode) {
    	this.specificTransactionCode = specificTransactionCode;
    }

    public Integer getOperatorNumber() {
        return operatorNumber;
    }

    public void setOperatorNumber(Integer operatorNumber) {
        this.operatorNumber = operatorNumber;
    }

    public String getTargetDepartmentCode() {
        return targetDepartmentCode;
    }

    public void setTargetDepartmentCode(String targetDepartmentCode) {
        this.targetDepartmentCode = targetDepartmentCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}