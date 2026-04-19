package c.y.queuing.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import c.y.queuing.dto.AnnouncementPayload;
import c.y.queuing.dto.DeptPayload;
import c.y.queuing.dto.DeptTicketView;
import c.y.queuing.dto.LobbyPayload;
import c.y.queuing.entity.Ticket;
import c.y.queuing.repository.DepartmentRepository;
import c.y.queuing.repository.SpecificTransactionRepository;
import c.y.queuing.repository.TicketRepository;
import c.y.queuing.repository.TransactionTypeRepository;

@Service
public class RealtimePushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SpecificTransactionRepository specificTransactionRepository;

    public RealtimePushService(
            SimpMessagingTemplate messagingTemplate,
            TicketRepository ticketRepository,
            DepartmentRepository departmentRepository,
            TransactionTypeRepository transactionTypeRepository,
            SpecificTransactionRepository specificTransactionRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.specificTransactionRepository = specificTransactionRepository;
    }

    public void pushLobbySnapshot() {
        List<Ticket> visible = ticketRepository.findByStatusInOrderByCreatedAtAsc(
                List.of("WAITING", "CALLED", "IN_SERVICE")
        );

        Map<String, List<Ticket>> grouped = visible.stream()
                .filter(t -> t.getDepartment() != null && !t.getDepartment().isBlank())
                .collect(Collectors.groupingBy(
                        t -> t.getDepartment().trim().toUpperCase(),
                        Collectors.toList()
                ));

        List<LobbyPayload.DepartmentView> departments = departmentRepository
                .findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(d -> {
                    String code = d.getCode().trim().toUpperCase();
                    String name = d.getName();
                    List<Ticket> deptTickets = grouped.getOrDefault(code, List.of());

                    LobbyPayload.TicketView nowServing = pickNowServing(deptTickets);
                    List<LobbyPayload.TicketView> waiting = deptTickets.stream()
                            .filter(t -> "WAITING".equals(t.getStatus()))
                            .map(t -> new LobbyPayload.TicketView(t.getQueueNo(), t.getStatus()))
                            .toList();

                    return new LobbyPayload.DepartmentView(code, name, nowServing, waiting);
                })
                .toList();

        messagingTemplate.convertAndSend("/topic/lobby", new LobbyPayload(departments));
    }

    public void pushAnnouncement(Long id, String queueNo, String department, String status) {
        if (!"CALLED".equals(status)) {
            return;
        }

        // 🔥 Convert code → name
        String deptName = departmentRepository
                .findByCode(department)
                .map(d -> d.getName())
                .orElse(department);

        AnnouncementPayload payload =
                new AnnouncementPayload(id, queueNo, deptName, status);

        messagingTemplate.convertAndSend("/topic/announcements", payload);
    }

    public void pushDeptSnapshot(String dept) {
        if (dept == null || dept.isBlank()) {
            return;
        }

        String deptUpper = dept.trim().toUpperCase();

        Map<String, String> txNameMap = transactionTypeRepository.findAll()
                .stream()
                .filter(tx -> tx.getCode() != null)
                .collect(Collectors.toMap(
                        tx -> tx.getCode().trim().toUpperCase(),
                        tx -> tx.getName(),
                        (a, b) -> a
                ));

        Map<String, String> specTxNameMap = specificTransactionRepository.findAll()
                .stream()
                .filter(st -> st.getCode() != null)
                .collect(Collectors.toMap(
                        st -> st.getCode().trim().toUpperCase(),
                        st -> st.getName(),
                        (a, b) -> a
                ));

        List<Ticket> activeTickets = ticketRepository
                .findByDepartmentAndStatusInOrderByCreatedAtAsc(
                        deptUpper, List.of("CALLED", "IN_SERVICE"));

        List<Ticket> waitingTickets = ticketRepository
                .findByDepartmentAndStatusOrderByCreatedAtAsc(deptUpper, "WAITING");

        List<DeptTicketView> active = activeTickets.stream()
                .map(t -> toDeptTicketView(t, txNameMap, specTxNameMap))
                .toList();

        List<DeptTicketView> waiting = waitingTickets.stream()
                .map(t -> toDeptTicketView(t, txNameMap, specTxNameMap))
                .toList();

        DeptPayload payload = new DeptPayload(deptUpper, active, waiting);
        messagingTemplate.convertAndSend("/topic/dept/" + deptUpper, payload);
    }

    private DeptTicketView toDeptTicketView(
            Ticket t,
            Map<String, String> txNameMap,
            Map<String, String> specTxNameMap
    ) {
        String txCode = safeUpper(t.getTransactionType());
        String specCode = safeUpper(t.getSpecificTransaction());

        String txName = txNameMap.getOrDefault(txCode, t.getTransactionType());
        String specName = specTxNameMap.getOrDefault(specCode, t.getSpecificTransaction());

        String txDisplay = txName != null ? txName : "";
        if (specName != null && !specName.isBlank()) {
            txDisplay += " - " + specName;
        }

        return new DeptTicketView(
                t.getId(),
                t.getQueueNo(),
                t.getStatus(),
                t.getFirstName(),
                t.getLastName(),
                t.getTransactionType(),
                t.getSpecificTransaction(),
                txDisplay,
                t.getOperatorNumber(),
                t.getDepartment()
        );
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private LobbyPayload.TicketView pickNowServing(List<Ticket> deptTickets) {
        for (Ticket t : deptTickets) {
            if ("IN_SERVICE".equals(t.getStatus())) {
                return new LobbyPayload.TicketView(t.getQueueNo(), t.getStatus());
            }
        }
        for (Ticket t : deptTickets) {
            if ("CALLED".equals(t.getStatus())) {
                return new LobbyPayload.TicketView(t.getQueueNo(), t.getStatus());
            }
        }
        return null;
    }

    // ============ REALTIME FOR ADMIN DASH =================
    public void pushAdminRefresh() {
        messagingTemplate.convertAndSend("/topic/admin", "refresh");
    }
}