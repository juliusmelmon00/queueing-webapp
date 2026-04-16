package c.y.queuing.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import c.y.queuing.entity.Department;
import c.y.queuing.entity.Ticket;
import c.y.queuing.entity.TicketExtraField;
import c.y.queuing.repository.DepartmentRepository;
import c.y.queuing.repository.TicketExtraFieldRepository;
import c.y.queuing.repository.TicketRepository;

@Controller
@RequestMapping("/status")
public class ClientStatusController {

    private final TicketRepository ticketRepository;
    private final TicketExtraFieldRepository ticketExtraFieldRepository;
    private final DepartmentRepository departmentRepository;

    public ClientStatusController(
            TicketRepository ticketRepository,
            TicketExtraFieldRepository ticketExtraFieldRepository,
            DepartmentRepository departmentRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketExtraFieldRepository = ticketExtraFieldRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String lookupPage(
            @RequestParam(required = false) String queueNo,
            @RequestParam(required = false) String lastName,
            Model model
    ) {
        String q = (queueNo == null ? "" : queueNo.trim().toUpperCase());
        String ln = (lastName == null ? "" : lastName.trim());

        if (q.isBlank() || ln.isBlank()) {
            return "client-status-lookup";
        }

        Optional<Ticket> opt = ticketRepository
                .findTopByQueueNoAndLastNameIgnoreCaseOrderByCreatedAtDesc(q, ln);

        if (opt.isEmpty()) {
            model.addAttribute("error", "Ticket not found. Please check your Queue Number and Last Name.");
            return "client-status-lookup";
        }

        Ticket t = opt.get();

        populateStatusModel(model, t);
        return "client-status-view";
    }

    @PostMapping
    public String lookup(
            @RequestParam String queueNo,
            @RequestParam String lastName,
            Model model
    ) {
        String q = (queueNo == null ? "" : queueNo.trim().toUpperCase());
        String ln = (lastName == null ? "" : lastName.trim());

        if (q.isBlank() || ln.isBlank()) {
            model.addAttribute("error", "Please enter Queue Number and Last Name.");
            return "client-status-lookup";
        }

        Optional<Ticket> opt = ticketRepository
                .findTopByQueueNoAndLastNameIgnoreCaseOrderByCreatedAtDesc(q, ln);

        if (opt.isEmpty()) {
            model.addAttribute("error", "Ticket not found. Please check your Queue Number and Last Name.");
            return "client-status-lookup";
        }

        Ticket t = opt.get();

        populateStatusModel(model, t);
        return "client-status-view";
    }

    private void populateStatusModel(Model model, Ticket t) {
        model.addAttribute("queueNo", t.getQueueNo());
        model.addAttribute("status", t.getStatus());
        model.addAttribute("statusMessage", toClientMessage(t.getStatus()));
        model.addAttribute("createdAt", t.getCreatedAt());

        String deptCode = t.getDepartment();
        String departmentDisplayName = deptCode;

        if (deptCode != null && !deptCode.isBlank()) {
            Optional<Department> deptOpt = departmentRepository.findByCode(deptCode);
            if (deptOpt.isPresent() && deptOpt.get().getName() != null && !deptOpt.get().getName().isBlank()) {
                departmentDisplayName = deptOpt.get().getName();
            }
        }

        model.addAttribute("department", departmentDisplayName);

        List<TicketExtraField> extraFields =
                ticketExtraFieldRepository.findByTicketIdOrderByIdAsc(t.getId());
        model.addAttribute("extraFields", extraFields);

        Optional<Ticket> nowServing = ticketRepository
                .findTopByDepartmentAndStatusOrderByCreatedAtAsc(deptCode, "IN_SERVICE");

        if (nowServing.isEmpty()) {
            nowServing = ticketRepository
                    .findTopByDepartmentAndStatusOrderByCreatedAtAsc(deptCode, "CALLED");
        }
        model.addAttribute("nowServing", nowServing.orElse(null));

        List<Ticket> aheadTickets = ticketRepository
                .findByDepartmentAndStatusAndQueueNoLessThanOrderByQueueNoAsc(deptCode, "WAITING", t.getQueueNo());

        int maxShow = 5;
        List<Ticket> aheadLimited = aheadTickets.size() > maxShow
                ? aheadTickets.subList(0, maxShow)
                : aheadTickets;

        model.addAttribute("aheadCount", aheadTickets.size());
        model.addAttribute("aheadList", aheadLimited);
    }

    private String toClientMessage(String status) {
        if (status == null) return "Status updated.";

        return switch (status) {
            case "WAITING" -> "Please wait. You are in queue.";
            case "CALLED" -> "You are being called. Please proceed when instructed.";
            case "IN_SERVICE" -> "You are currently being served.";
            case "DONE" -> "Your transaction is complete. Thank you!";
            default -> "Status updated.";
        };
    }
}