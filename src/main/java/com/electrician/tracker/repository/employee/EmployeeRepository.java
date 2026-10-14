package com.electrician.tracker.repository.employee;

import java.util.List;

import com.electrician.tracker.domain.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByActiveTrue();
}
