package c.y.queuing.dto;

public class DeptTicketView {
    private Long id;
    private String queueNo;
    private String status;
    private String firstName;
    private String lastName;
    private String transactionType;
    private String specificTransaction;
    private String txDisplay;
    private Integer operatorNumber;
    private String department;

    public DeptTicketView() {}

    public DeptTicketView(
            Long id,
            String queueNo,
            String status,
            String firstName,
            String lastName,
            String transactionType,
            String specificTransaction,
            String txDisplay,
            Integer operatorNumber,
            String department
    ) {
        this.id = id;
        this.queueNo = queueNo;
        this.status = status;
        this.firstName = firstName;
        this.lastName = lastName;
        this.transactionType = transactionType;
        this.specificTransaction = specificTransaction;
        this.txDisplay = txDisplay;
        this.operatorNumber = operatorNumber;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getQueueNo() {
        return queueNo;
    }

    public String getStatus() {
        return status;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getSpecificTransaction() {
        return specificTransaction;
    }

    public String getTxDisplay() {
        return txDisplay;
    }

    public Integer getOperatorNumber() {
        return operatorNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQueueNo(String queueNo) {
        this.queueNo = queueNo;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setSpecificTransaction(String specificTransaction) {
        this.specificTransaction = specificTransaction;
    }

    public void setTxDisplay(String txDisplay) {
        this.txDisplay = txDisplay;
    }

    public void setOperatorNumber(Integer operatorNumber) {
        this.operatorNumber = operatorNumber;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}