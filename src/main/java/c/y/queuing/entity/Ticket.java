package c.y.queuing.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String queueNo;
    private String firstName;
    private String lastName;
    private String transactionType;
    private String specificTransaction;
    private Integer operatorNumber;
    private String status;
    private String department;
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "WAITING";
        }

        // No hardcoded department fallback here.
        // Department must be assigned by routing logic before save.
        if (department != null) {
            department = department.trim().toUpperCase();
        }

        if (transactionType != null) {
            transactionType = transactionType.trim().toUpperCase();
        }

        if (specificTransaction != null && !specificTransaction.isBlank()) {
            specificTransaction = specificTransaction.trim().toUpperCase();
        } else {
            specificTransaction = null;
        }

        if (queueNo != null) {
            queueNo = queueNo.trim().toUpperCase();
        }
    }

    public Long getId() {
        return id;
    }

    public String getQueueNo() {
        return queueNo;
    }

    public void setQueueNo(String queueNo) {
        this.queueNo = queueNo;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSpecificTransaction() {
        return specificTransaction;
    }

    public void setSpecificTransaction(String specificTransaction) {
        this.specificTransaction = specificTransaction;
    }

    public Integer getOperatorNumber() {
        return operatorNumber;
    }

    public void setOperatorNumber(Integer operatorNumber) {
        this.operatorNumber = operatorNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}