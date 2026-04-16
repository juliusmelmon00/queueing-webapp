package c.y.queuing.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import c.y.queuing.entity.Ticket;
import c.y.queuing.repository.DepartmentRepository;
import c.y.queuing.repository.SpecificTransactionRepository;
import c.y.queuing.repository.TicketExtraFieldRepository;
import c.y.queuing.repository.TicketHistoryRepository;
import c.y.queuing.repository.TicketRepository;
import c.y.queuing.repository.TransactionTypeRepository;
import c.y.queuing.security.DeptRoleHelper;
import c.y.queuing.service.DeptQueueService;
import c.y.queuing.service.HistoryService;
import c.y.queuing.service.RealtimePushService;

@Controller
@RequestMapping("/dept")
public class DeptController {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository historyRepository;
    private final HistoryService historyService;
    private final RealtimePushService realtimePushService;
    private final DeptRoleHelper deptRoleHelper;
    private final DeptQueueService deptQueueService;
    private final DepartmentRepository departmentRepository;
    private final TicketExtraFieldRepository ticketExtraFieldRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SpecificTransactionRepository specificTransactionRepository;

    public DeptController(
            TicketRepository ticketRepository,
            TicketHistoryRepository historyRepository,
            HistoryService historyService,
            RealtimePushService realtimePushService,
            DeptRoleHelper deptRoleHelper,
            DeptQueueService deptQueueService,
            DepartmentRepository departmentRepository,
            TicketExtraFieldRepository ticketExtraFieldRepository,
            TransactionTypeRepository transactionTypeRepository,
            SpecificTransactionRepository specificTransactionRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.historyRepository = historyRepository;
        this.historyService = historyService;
        this.realtimePushService = realtimePushService;
        this.deptRoleHelper = deptRoleHelper;
        this.deptQueueService = deptQueueService;
        this.departmentRepository = departmentRepository;
        this.ticketExtraFieldRepository = ticketExtraFieldRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.specificTransactionRepository = specificTransactionRepository;
    }

    @GetMapping("")
    public String redirectDept(Authentication auth) {
        if (deptRoleHelper.isAdmin(auth)) {
            return "redirect:/admin/config";
        }

        String userDept = deptRoleHelper.resolveDept(auth);
        if (userDept != null) {
            return "redirect:/dept/" + userDept;
        }

        return "redirect:/login";
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @GetMapping("/{dept}")
    public String departmentPage(@PathVariable String dept, Model model, Authentication auth) {

        String deptUpper = dept.toUpperCase();

        boolean deptExists = departmentRepository.findByCode(deptUpper).isPresent();
        if (!deptExists) {
            if (deptRoleHelper.isAdmin(auth)) {
                return "redirect:/admin/config?invalidDept";
            }
            return "redirect:/dept";
        }

        String departmentName = departmentRepository.findByCode(deptUpper)
                .map(d -> d.getName())
                .orElse(deptUpper);

        List<Ticket> active = ticketRepository
                .findByDepartmentAndStatusInOrderByCreatedAtAsc(
                        deptUpper, List.of("CALLED", "IN_SERVICE"));

        List<Ticket> waiting = ticketRepository
                .findByDepartmentAndStatusOrderByCreatedAtAsc(deptUpper, "WAITING");

        boolean hasActive = !active.isEmpty();
        Long nextTicketId = waiting.isEmpty() ? null : waiting.get(0).getId();

        Map<String, String> txNameMap = transactionTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        tx -> tx.getCode().trim().toUpperCase(),
                        tx -> tx.getName(),
                        (a, b) -> a
                ));

        Map<String, String> specTxNameMap = specificTransactionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        st -> st.getCode().trim().toUpperCase(),
                        st -> st.getName(),
                        (a, b) -> a
                ));

        model.addAttribute("txNameMap", txNameMap);
        model.addAttribute("specTxNameMap", specTxNameMap);

        model.addAttribute("nextTicketId", nextTicketId);
        model.addAttribute("dept", deptUpper);
        model.addAttribute("departmentName", departmentName);
        model.addAttribute("active", active);
        model.addAttribute("waiting", waiting);
        model.addAttribute("hasActive", hasActive);
        model.addAttribute("isAdmin", deptRoleHelper.isAdmin(auth));
        model.addAttribute("userDept", deptRoleHelper.resolveDept(auth));

        model.addAttribute(
                "departments",
                departmentRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                        .stream()
                        .map(d -> d.getCode())
                        .collect(Collectors.toList())
        );

        return "dept";
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @PostMapping("/{dept}/call/{id}")
    public String call(@PathVariable String dept, @PathVariable Long id, Authentication auth) {
        String deptUpper = dept.toUpperCase();

        if (departmentRepository.findByCode(deptUpper).isEmpty()) {
            return "redirect:/admin/config?invalidDept";
        }

        String actor = (auth != null ? auth.getName() : "UNKNOWN");
        return deptQueueService.callTicket(deptUpper, id, actor);
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @PostMapping("/{dept}/next")
    public String next(@PathVariable String dept, Authentication auth) {
        String deptUpper = dept.toUpperCase();

        if (departmentRepository.findByCode(deptUpper).isEmpty()) {
            return "redirect:/admin/config?invalidDept";
        }

        String actor = (auth != null ? auth.getName() : "UNKNOWN");
        return deptQueueService.nextTicket(deptUpper, actor);
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @PostMapping("/{dept}/in-service/{id}")
    public String inService(@PathVariable String dept, @PathVariable Long id, Authentication auth) {

        String deptUpper = dept.toUpperCase();
        if (departmentRepository.findByCode(deptUpper).isEmpty()) {
            return "redirect:/admin/config?invalidDept";
        }

        String actor = (auth != null ? auth.getName() : "UNKNOWN");

        Ticket t = ticketRepository.findById(id).orElseThrow();

        if (t.getDepartment() == null || !deptUpper.equals(t.getDepartment())) {
            return "redirect:/dept/" + deptUpper;
        }

        if (!"CALLED".equals(t.getStatus())) {
            return "redirect:/dept/" + deptUpper;
        }

        Ticket before = snapshot(t);

        t.setStatus("IN_SERVICE");
        Ticket after = ticketRepository.save(t);

        historyService.log("IN_SERVICE", before, after, actor, null);

        realtimePushService.pushLobbySnapshot();
        realtimePushService.pushDeptSnapshot(deptUpper);

        return "redirect:/dept/" + deptUpper;
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @PostMapping("/{dept}/done/{id}")
    public String done(@PathVariable String dept, @PathVariable Long id, Authentication auth) {

        String deptUpper = dept.toUpperCase();
        if (departmentRepository.findByCode(deptUpper).isEmpty()) {
            return "redirect:/admin/config?invalidDept";
        }

        String actor = (auth != null ? auth.getName() : "UNKNOWN");

        Ticket t = ticketRepository.findById(id).orElseThrow();

        if (t.getDepartment() == null || !deptUpper.equals(t.getDepartment())) {
            return "redirect:/dept/" + deptUpper;
        }

        if (!"IN_SERVICE".equals(t.getStatus())) {
            return "redirect:/dept/" + deptUpper;
        }

        Ticket before = snapshot(t);

        t.setStatus("DONE");
        Ticket after = ticketRepository.save(t);

        historyService.log("DONE", before, after, actor, null);

        realtimePushService.pushLobbySnapshot();
        realtimePushService.pushDeptSnapshot(deptUpper);

        return "redirect:/dept/" + deptUpper;
    }

    @PreAuthorize("@deptAccessService.canAccessDept(authentication, #dept)")
    @GetMapping("/{dept}/history/{id}")
    public String viewHistory(
            @PathVariable String dept,
            @PathVariable Long id,
            @RequestParam(required = false) String source,
            Model model
    ) {

        String deptUpper = dept.toUpperCase();
        if (departmentRepository.findByCode(deptUpper).isEmpty()) {
            return "redirect:/admin/config?invalidDept";
        }

        Ticket ticket = ticketRepository.findById(id).orElseThrow();

        boolean isAdminView = "admin".equalsIgnoreCase(source);

        // 🔥 BUILD NAME MAPS (same as dashboard)
        Map<String, String> txNameMap = transactionTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        tx -> tx.getCode().trim().toUpperCase(),
                        tx -> tx.getName(),
                        (a, b) -> a
                ));

        Map<String, String> specTxNameMap = specificTransactionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        st -> st.getCode().trim().toUpperCase(),
                        st -> st.getName(),
                        (a, b) -> a
                ));

        // 🔥 RESOLVE DISPLAY NAMES
        String txCode = ticket.getTransactionType() != null
                ? ticket.getTransactionType().trim().toUpperCase()
                : "";

        String specCode = ticket.getSpecificTransaction() != null
                ? ticket.getSpecificTransaction().trim().toUpperCase()
                : "";

        String txDisplay = txNameMap.getOrDefault(txCode, ticket.getTransactionType());

        String specDisplay = (specCode.isBlank())
                ? "-"
                : specTxNameMap.getOrDefault(specCode, ticket.getSpecificTransaction());

        // 🔥 ADD TO MODEL
        model.addAttribute("dept", deptUpper);
        model.addAttribute("ticket", ticket);
        model.addAttribute("txDisplay", txDisplay);
        model.addAttribute("specDisplay", specDisplay);

        model.addAttribute("history", historyRepository.findByTicketIdOrderByCreatedAtDesc(id));
        model.addAttribute("isAdminView", isAdminView);

        model.addAttribute("extraFields",
                ticketExtraFieldRepository.findByTicketIdOrderByIdAsc(id));

        return "history";
    }

    private Ticket snapshot(Ticket t) {
        Ticket x = new Ticket();
        x.setDepartment(t.getDepartment());
        x.setStatus(t.getStatus());
        x.setQueueNo(t.getQueueNo());
        x.setFirstName(t.getFirstName());
        x.setLastName(t.getLastName());
        x.setTransactionType(t.getTransactionType());
        x.setSpecificTransaction(t.getSpecificTransaction());
        x.setOperatorNumber(t.getOperatorNumber());
        return x;
    }
}