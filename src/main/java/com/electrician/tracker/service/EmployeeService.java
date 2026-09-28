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

/**
 * Employees. A MANAGER never sees or sets wages: employees they add start
 * with a zero wage (the ADMIN fills it in), their edits keep the stored wage.
 * Only an ADMIN deletes employees.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AccessControl accessControl;
    private final FinancialDataMasker masker;

    public EmployeeService(EmployeeRepository employeeRepository, AttendanceRepository attendanceRepository,
            AccessControl accessControl, FinancialDataMasker masker) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.accessControl = accessControl;
        this.masker = masker;
    }

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return masker.maskEmployees(employeeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllActive() {
        return masker.maskEmployees(employeeRepository.findByActiveTrue());
    }

    @Transactional(readOnly = true)
    public Employee findById(Long id) {
        return masker.maskEmployee(findEntity(id));
    }

    @Transactional
    public Employee create(Employee employee) {
        if (!accessControl.canViewFinancials()) {
            employee.setDefaultDailyWage(BigDecimal.ZERO);
        }
        validate(employee);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, Employee changes) {
        Employee existing = findEntity(id);
        if (!accessControl.canViewFinancials()) {
            changes.setDefaultDailyWage(existing.getDefaultDailyWage());
        }
        validate(changes);
        existing.setName(changes.getName());
        existing.setDefaultDailyWage(changes.getDefaultDailyWage());
        existing.setMaster(changes.isMaster());
        existing.setActive(changes.isActive());
        return existing;
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        findEntity(id).setActive(active);
    }

    @Transactional
    public void delete(Long id) {
        accessControl.requireAdmin();
        long attendanceCount = attendanceRepository.countByEmployeeId(id);
        if (attendanceCount > 0) {
            throw new ReferencedEntityException("error.employee.delete.hasAttendance", attendanceCount);
        }
        employeeRepository.deleteById(id);
    }

    private Employee findEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.employee.notFound"));
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
