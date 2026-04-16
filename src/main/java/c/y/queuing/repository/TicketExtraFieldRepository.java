package c.y.queuing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.TicketExtraField;

public interface TicketExtraFieldRepository extends JpaRepository<TicketExtraField, Long> {

    List<TicketExtraField> findByTicketIdOrderByIdAsc(Long ticketId);
    
}