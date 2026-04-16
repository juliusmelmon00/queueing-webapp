package c.y.queuing.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import c.y.queuing.entity.QueueCounter;
import c.y.queuing.repository.QueueCounterRepository;

@Service
public class QueueNumberService {
	private final QueueCounterRepository repo;
	
	public QueueNumberService(QueueCounterRepository repo) {
		this.repo = repo;
	}
	
	@Transactional
	public String nextQueueNumber() {
		
		LocalDate today = LocalDate.now();
		
		QueueCounter counter = repo.findByCounterDate(today)
				.orElseGet(() -> {
					QueueCounter c = new QueueCounter();
					c.setCounterDate(today);
					c.setLastNumber(0L);
					return c;
				});
		long next = counter.getLastNumber() + 1;
		counter.setLastNumber(next);
		
		repo.save(counter);
		
		return "A" + String.format("%03d", next);
	}

}
