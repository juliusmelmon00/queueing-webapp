package c.y.queuing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.TicketHistory;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
