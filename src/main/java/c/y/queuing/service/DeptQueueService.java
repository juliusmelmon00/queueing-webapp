package c.y.queuing.service;

import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import c.y.queuing.entity.Ticket;
import c.y.queuing.repository.TicketRepository;

@Service
public class DeptQueueService {
	private final TicketRepository ticketRepository;
	private final HistoryService historyService;
	private final RealtimePushService realtimePushService;
	
	public DeptQueueService(TicketRepository ticketRepository, HistoryService historyService, RealtimePushService realtimePushService) {
		
		this.ticketRepository = ticketRepository;
		this.historyService = historyService;
		this.realtimePushService = realtimePushService;
	}
	
	@Transactional
	public String callTicket(String dept, Long id, String actor) {
		String deptUpper = dept.toUpperCase();
		
		Ticket t = ticketRepository.findByIdForUpdate(id).orElseThrow();
		
		// prevent URL tampering
		if (t.getDepartment() == null || !deptUpper.equals(t.getDepartment())) {
			return "redirect:/dept/" + deptUpper;
		}
		
		// only WAITING can be called
		if (!"WAITING".equals(t.getStatus())) {
			return "redirect:/dept/" + deptUpper;
		}
		// lock active tickets in the dept before deciding
		List<Ticket> activeLocked = ticketRepository.findActiveTicketsForUpdate(
				deptUpper, List.of("CALLED", "IN_SERVICE")
				);
		if (!activeLocked.isEmpty()) {
			return "redirect:/dept/" + deptUpper + "?activeExists=1";
		}
		
		Ticket before = snapshot(t);
		
		t.setStatus("CALLED");
		Ticket after = ticketRepository.save(t);
		
		historyService.log("CALL", before, after, actor, null);
		
		realtimePushService.pushLobbySnapshot();
		realtimePushService.pushDeptSnapshot(deptUpper);
		realtimePushService.pushAnnouncement(
				after.getId(),
				after.getQueueNo(),
				after.getDepartment(),
				after.getStatus() );
		return "redirect:/dept/" + deptUpper;
	}
	
	@Transactional
	public String nextTicket(String dept, String actor) {
		String deptUpper = dept.toUpperCase();
		
		// lock active ticket first
		List<Ticket> activeLocked = ticketRepository.findActiveTicketsForUpdate(
				deptUpper, List.of("CALLED", "IN_SERVICE"));
		
		if (!activeLocked.isEmpty()) {
			return "redirect:/dept/" + deptUpper + "?activeExists=1";
		}
		
		// lock oldest waiting ticket
		Ticket t = ticketRepository
		        .findFirstByDepartmentAndStatusOrderByCreatedAtAsc(deptUpper, "WAITING")
		        .orElse(null);
		
				/*.stream()
				.findFirst()
				.orElse(null);*/
		if (t == null) {
			return "redirect:/dept/" + deptUpper + "?noWaiting=1";
		}
		
		Ticket before = snapshot(t);
		
		t.setStatus("CALLED");
		Ticket after = ticketRepository.save(t);
		
		historyService.log("NEXT_CALL", before, after, actor, "Auto-called oldest WAITING");
		
		realtimePushService.pushLobbySnapshot();
		realtimePushService.pushDeptSnapshot(deptUpper);
		realtimePushService.pushAnnouncement(
				after.getId(),
				after.getQueueNo(),
				after.getDepartment(),
				after.getStatus());
		
		return "redirect:/dept/" + deptUpper;

	}
	private Ticket snapshot(Ticket t) {
		Ticket x = new Ticket();
		x.setDepartment(t.getDepartment());
		x.setStatus(t.getStatus());
		x.setQueueNo(t.getQueueNo());
		x.setFirstName(t.getFirstName());
		x.setLastName(t.getLastName());
		x.setTransactionType(t.getTransactionType());
		x.setOperatorNumber(t.getOperatorNumber());
		return x;
		
	}
	
	
	

}
