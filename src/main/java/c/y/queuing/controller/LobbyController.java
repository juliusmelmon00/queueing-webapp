package c.y.queuing.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import c.y.queuing.dto.LobbyPayload;
import c.y.queuing.entity.SystemSetting;
import c.y.queuing.entity.Ticket;
import c.y.queuing.repository.DepartmentRepository;
import c.y.queuing.repository.SystemSettingRepository;
import c.y.queuing.repository.TicketRepository;

@Controller
public class LobbyController {

    private final TicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;
    private final SystemSettingRepository systemSettingRepository;

    public LobbyController(TicketRepository ticketRepository,
                           DepartmentRepository departmentRepository,
                           SystemSettingRepository systemSettingRepository) {
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping("/lobby")
    public String lobby(Model model) {

        List<Ticket> visible = ticketRepository.findByStatusInOrderByCreatedAtAsc(
                List.of("WAITING", "CALLED", "IN_SERVICE")
        );

        Map<String, List<Ticket>> grouped = visible.stream()
                .filter(t -> t.getDepartment() != null && !t.getDepartment().isBlank())
                .collect(Collectors.groupingBy(
                        t -> t.getDepartment().toUpperCase(),
                        Collectors.toList()
                ));

        List<LobbyPayload.DepartmentView> departments = departmentRepository
                .findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(d -> {
                    String code = d.getCode().toUpperCase();
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

        String companyName = systemSettingRepository.findBySettingKey("company_name")
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .orElse("Lobby Monitor");
        
        String companyLogoUrl = systemSettingRepository.findBySettingKey("company_logo")
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .orElse("");
        
        model.addAttribute("departments", departments);
        model.addAttribute("companyName", companyName);
        model.addAttribute("companyLogoUrl", companyLogoUrl);

        return "lobby";
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
}