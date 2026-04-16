package c.y.queuing.service;

import org.springframework.stereotype.Service;

import c.y.queuing.entity.Ticket;
import c.y.queuing.entity.TicketHistory;
import c.y.queuing.repository.TicketHistoryRepository;

@Service
public class HistoryService {

    private final TicketHistoryRepository historyRepo;

    public HistoryService(TicketHistoryRepository historyRepo) {
        this.historyRepo = historyRepo;
    }

    public void log(String action, Ticket before, Ticket after, String performedBy, String note) {
        TicketHistory h = new TicketHistory();
        h.setTicketId(after.getId());
        h.setAction(action);

        h.setFromDepartment(before == null ? null : before.getDepartment());
        h.setToDepartment(after.getDepartment());

        h.setFromStatus(before == null ? null : before.getStatus());
        h.setToStatus(after.getStatus());

        h.setPerformedBy(performedBy == null ? "SYSTEM" : performedBy);
        h.setNote(note);

        historyRepo.save(h);
    }
}
