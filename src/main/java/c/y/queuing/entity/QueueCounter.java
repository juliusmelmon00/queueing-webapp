package c.y.queuing.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "queue_counter")
public class QueueCounter {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private LocalDate counterDate;
	private Long lastNumber;
	public Long getId() { return id; }
	
	public LocalDate getCounterDate() { return counterDate; }
	public void setCounterDate(LocalDate counterDate) { this.counterDate = counterDate; }
	
	public Long getLastNumber() { return lastNumber; }
	public void setLastNumber(Long lastNumber) { this.lastNumber = lastNumber; }
 }
