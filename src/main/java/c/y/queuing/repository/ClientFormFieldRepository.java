package c.y.queuing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.ClientFormField;

public interface ClientFormFieldRepository extends JpaRepository<ClientFormField, Long> {

    List<ClientFormField> findAllByOrderByDisplayOrderAsc();

    List<ClientFormField> findByIsActiveTrueOrderByDisplayOrderAsc();

    boolean existsByFieldCode(String fieldCode);
}