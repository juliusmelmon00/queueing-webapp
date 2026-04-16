package c.y.queuing.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allow_routing", nullable = false)
    private Boolean allowRouting = true;

    @Column(name = "one_active_ticket_only", nullable = false)
    private Boolean oneActiveTicketOnly = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getAllowRouting() {
        return allowRouting;
    }

    public void setAllowRouting(Boolean allowRouting) {
        this.allowRouting = allowRouting;
    }

    public Boolean getOneActiveTicketOnly() {
        return oneActiveTicketOnly;
    }

    public void setOneActiveTicketOnly(Boolean oneActiveTicketOnly) {
        this.oneActiveTicketOnly = oneActiveTicketOnly;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}