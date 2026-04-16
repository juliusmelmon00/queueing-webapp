package c.y.queuing.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.QueueCounter;

public interface QueueCounterRepository extends JpaRepository<QueueCounter, Long> {
	
	Optional<QueueCounter> findByCounterDate(LocalDate counterDate);
}