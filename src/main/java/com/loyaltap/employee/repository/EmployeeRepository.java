package com.loyaltap.employee.repository;

import com.loyaltap.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByBusinessIdAndUserId(UUID businessId, String userId);

    List<Employee> findAllByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    Optional<Employee> findByIdAndBusinessId(UUID employeeId, UUID businessId);
}
