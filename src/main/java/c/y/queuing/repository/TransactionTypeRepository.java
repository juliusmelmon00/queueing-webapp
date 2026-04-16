package c.y.queuing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import c.y.queuing.entity.TransactionType;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, Long> {
	List<TransactionType> findByIsActiveTrueOrderByDisplayOrderAsc();
	List<TransactionType> findAllByOrderByDisplayOrderAsc();
	Optional<TransactionType> findByCode(String code);
	boolean existsByCode(String code);

}
