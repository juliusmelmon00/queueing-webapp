package c.y.queuing.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import c.y.queuing.entity.SpecificTransaction;
import c.y.queuing.entity.Ticket;
import c.y.queuing.entity.TicketExtraField;
import c.y.queuing.repository.ClientFormFieldRepository;
import c.y.queuing.repository.SpecificTransactionRepository;
import c.y.queuing.repository.TicketExtraFieldRepository;
import c.y.queuing.repository.TicketRepository;
import c.y.queuing.repository.TransactionTypeRepository;
import c.y.queuing.service.HistoryService;
import c.y.queuing.service.QueueNumberService;
import c.y.queuing.service.RealtimePushService;
import c.y.queuing.service.RoutingRuleService;
import jakarta.servlet.http.HttpSession;



@Controller
public class ClientController {

    private final TicketRepository ticketRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final SpecificTransactionRepository specificTransactionRepository;
    private final HistoryService historyService;
    private final RealtimePushService realtimePushService;
    private final QueueNumberService queueNumberService;
    private final RoutingRuleService routingRuleService;
    private final ClientFormFieldRepository clientFormFieldRepository;
    private final TicketExtraFieldRepository ticketExtraFieldRepository;

    public ClientController(
            TicketRepository ticketRepository,
            TransactionTypeRepository transactionTypeRepository,
            SpecificTransactionRepository specificTransactionRepository,
            HistoryService historyService,
            RealtimePushService realtimePushService,
            QueueNumberService queueNumberService,
            RoutingRuleService routingRuleService,
            ClientFormFieldRepository clientFormFieldRepository,
            TicketExtraFieldRepository ticketExtraFieldRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.specificTransactionRepository = specificTransactionRepository;
        this.historyService = historyService;
        this.realtimePushService = realtimePushService;
        this.queueNumberService = queueNumberService;
        this.routingRuleService = routingRuleService;
        this.clientFormFieldRepository = clientFormFieldRepository;
        this.ticketExtraFieldRepository = ticketExtraFieldRepository;
    }

    @GetMapping("/client")
    public String clientPage(HttpSession session, Model model) {
        Boolean allowed = (Boolean) session.getAttribute(EntryController.QR_ALLOWED);
        if (allowed == null || !allowed) {
            return "redirect:/entry-denied";
        }

        loadClientPageData(model);
        return "client";
    }

    @GetMapping("/api/specific-transactions")
    @ResponseBody
    public List<SpecificTransaction> getSpecificTransactions(@RequestParam String tx) {
        return specificTransactionRepository
                .findByTransactionTypeCodeAndIsActiveTrueOrderByDisplayOrderAsc(tx);
    }

    @PostMapping("/client/submit")
    public String submitTicket(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String transactionType,
            @RequestParam(required = false) String specificTransaction,
            @RequestParam Map<String, String> allParams,
            HttpSession session,
            Model model
    ) {
        Boolean allowed = (Boolean) session.getAttribute(EntryController.QR_ALLOWED);
        if (allowed == null || !allowed) {
            return "redirect:/entry-denied";
        }

        loadClientPageData(model);

        Long lastSubmit = (Long) session.getAttribute("LAST_SUBMIT_TIME");
        long now = System.currentTimeMillis();

        if (lastSubmit != null && now - lastSubmit < 5000) {
            model.addAttribute("error", "Please wait before creating another ticket.");
            return "client";
        }
        session.setAttribute("LAST_SUBMIT_TIME", now);

        if (session.getAttribute("CLIENT_TICKET_CREATED") != null) {
            model.addAttribute("error", "You already created a ticket.");
            return "client";
        }

        Ticket ticket = new Ticket();
        ticket.setFirstName(firstName);
        ticket.setLastName(lastName);
        ticket.setTransactionType(transactionType);
        ticket.setSpecificTransaction(specificTransaction);

        String resolvedDepartment;
        try {
            resolvedDepartment = routingRuleService.resolveDepartment(transactionType, specificTransaction);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "client";
        }

        if (resolvedDepartment == null || resolvedDepartment.isBlank()) {
            model.addAttribute("error", "No routing rule found for the selected transaction.");
            return "client";
        }

        ticket.setDepartment(resolvedDepartment.toUpperCase().trim());
        ticket.setOperatorNumber(null);
        ticket.setQueueNo(queueNumberService.nextQueueNumber());

        Ticket saved = ticketRepository.saveAndFlush(ticket);

        clientFormFieldRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .forEach(field -> {
                    String key = "extra_" + field.getFieldCode();
                    String value = allParams.get(key);

                    if (value != null && !value.isBlank()) {
                        TicketExtraField extra = new TicketExtraField();
                        extra.setTicketId(saved.getId());
                        extra.setFieldCode(field.getFieldCode());
                        extra.setFieldLabel(field.getLabel());
                        extra.setFieldValue(value.trim());

                        ticketExtraFieldRepository.save(extra);
                    }
                });

        session.setAttribute("CLIENT_TICKET_CREATED", true);

        historyService.log("CREATED", null, saved, "CLIENT", "Ticket created by client");

        realtimePushService.pushLobbySnapshot();
        realtimePushService.pushDeptSnapshot(saved.getDepartment());

        return "redirect:/status?queueNo=" + saved.getQueueNo() + "&lastName=" + saved.getLastName();
    }

    private void loadClientPageData(Model model) {
        model.addAttribute("transactionTypes",
                transactionTypeRepository.findByIsActiveTrueOrderByDisplayOrderAsc());

        model.addAttribute("clientFields",
                clientFormFieldRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }
}