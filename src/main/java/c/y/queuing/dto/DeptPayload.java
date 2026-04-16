package c.y.queuing.dto;

import java.util.List;

public class DeptPayload {
    private String dept;
    private List<DeptTicketView> active;
    private List<DeptTicketView> waiting;

    public DeptPayload() {}

    public DeptPayload(String dept, List<DeptTicketView> active, List<DeptTicketView> waiting) {
        this.dept = dept;
        this.active = active;
        this.waiting = waiting;
    }

    public String getDept() { return dept; }
    public List<DeptTicketView> getActive() { return active; }
    public List<DeptTicketView> getWaiting() { return waiting; }

    public void setDept(String dept) { this.dept = dept; }
    public void setActive(List<DeptTicketView> active) { this.active = active; }
    public void setWaiting(List<DeptTicketView> waiting) { this.waiting = waiting; }
}