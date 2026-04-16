package c.y.queuing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Department> findByIsActiveTrueAndAllowRoutingTrueOrderByDisplayOrderAsc();

    List<Department> findAllByOrderByDisplayOrderAsc();

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);
}