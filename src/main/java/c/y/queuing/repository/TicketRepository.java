package c.y.queuing.repository;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import c.y.queuing.entity.Ticket;
import jakarta.persistence.LockModeType;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // ===== BASIC =====
    List<Ticket> findByStatusOrderByCreatedAtAsc(String status);

    // ✅ One-active-ticket rule helper
    boolean existsByDepartmentAndStatusIn(String department, List<String> statuses);

    // ===== FOR LOBBY SNAPSHOT =====
    List<Ticket> findByStatusInOrderByCreatedAtAsc(List<String> statuses);

    // ===== DEPT DASHBOARD =====
    List<Ticket> findByDepartmentAndStatusOrderByCreatedAtAsc(String department, String status);
    List<Ticket> findByDepartmentAndStatusInOrderByCreatedAtAsc(String department, List<String> statuses);

    // ✅ FIFO (oldest waiting in a department) - used by NEXT button
    Optional<Ticket> findFirstByDepartmentAndStatusOrderByCreatedAtAsc(String department, String status);

    // ===== CLIENT STATUS / TRACKING =====
    Optional<Ticket> findTopByQueueNoAndLastNameIgnoreCaseOrderByCreatedAtDesc(String queueNo, String lastname);

    // ===== OPTIONAL QUERIES (you already have these) =====
    Optional<Ticket> findTopByStatusAndQueueNoLessThanOrderByQueueNoDesc(String status, String queueNo);
    List<Ticket> findByDepartmentAndStatusAndQueueNoLessThanOrderByQueueNoAsc(String department, String status, String queueNo);

    Optional<Ticket> findTopByDepartmentAndStatusOrderByCreatedAtAsc(String department, String status);
    Optional<Ticket> findTopByOrderByIdDesc();
    
    // ====== FIFO =======
  // Optional<Ticket> findFirstByDepartmentAndStatusOrderByCreatedAtAsc(String department, String status);
    
 // =========================
 // TRANSACTION-SAFE LOCKING
 // =========================

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select t from Ticket t where t.id = :id")
 Optional<Ticket> findByIdForUpdate(Long id);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("""
        select t
        from Ticket t
        where t.department = :department
          and t.status in :statuses
        order by t.createdAt asc
        """)
 List<Ticket> findActiveTicketsForUpdate(String department, List<String> statuses);
 
 //================ AUDIT TRAIL FOR ADMIN CONFIG ================
 List<Ticket> findByCreatedAtBetweenOrderByCreatedAtAsc(
		 java.time.LocalDateTime start,
		 java.time.LocalDateTime end);
 long countByCreatedAtBetween(
	        java.time.LocalDateTime start,
	        java.time.LocalDateTime end
	);
 
 
 // ===== FOR FILTERING AUDIT TRAIL FOR ADMIN===========
 List<Ticket> findByCreatedAtBetweenAndStatusOrderByCreatedAtAsc(
		 java.time.LocalDateTime start,
		 java.time.LocalDateTime end,
		 String status);
 long countByCreatedAtBetweenAndStatus(
	        java.time.LocalDateTime start,
	        java.time.LocalDateTime end,
	        String status
	);
 
 @Query("""
 		select t.department, count(t)
 		from Ticket t
 		where t.createdAt >= :start
 		and t.createdAt < :end
 		group by t.department
 		order by t.department asc
 		""")
 List<Object[]> countTicketsPerDepartment(
		 java.time.LocalDateTime start,
		 java.time.LocalDateTime end);


 
 
 
 
 
 
 
 
 
 
 
 
 
}