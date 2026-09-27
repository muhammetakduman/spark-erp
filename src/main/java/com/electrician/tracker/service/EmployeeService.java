package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.EmployeeRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public EmployeeService(EmployeeRepository employeeRepository, AttendanceRepository attendanceRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllActive() {
        return employeeRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.employee.notFound"));
    }

    @Transactional
    public Employee create(Employee employee) {
        validate(employee);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, Employee changes) {
        validate(changes);
        Employee existing = findById(id);
        existing.setName(changes.getName());
        existing.setDefaultDailyWage(changes.getDefaultDailyWage());
        existing.setMaster(changes.isMaster());
        existing.setActive(changes.isActive());
        return existing;
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        findById(id).setActive(active);
    }

    @Transactional
    public void delete(Long id) {
        long attendanceCount = attendanceRepository.countByEmployeeId(id);
        if (attendanceCount > 0) {
            throw new ReferencedEntityException("error.employee.delete.hasAttendance", attendanceCount);
        }
        employeeRepository.deleteById(id);
    }

    private void validate(Employee employee) {
        if (employee.getName() == null || employee.getName().isBlank()) {
            throw new ValidationException("error.employee.name.required");
        }
        if (employee.getDefaultDailyWage() == null || employee.getDefaultDailyWage().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("error.employee.wage.negative");
        }
    }
}
