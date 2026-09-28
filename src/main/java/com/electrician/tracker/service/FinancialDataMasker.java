package com.electrician.tracker.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

/**
 * Removes what a MANAGER must not see before a service returns data: cost,
 * profit, receivables, payments, purchase prices, suppliers and wages. Hiding
 * a column in the UI is not enough, so the values never leave the service
 * layer. For an ADMIN every method returns its input unchanged.
 * <p>
 * Entities are detached before their fields are cleared, so a cleared value
 * can never be flushed back to the database.
 */
@Component
public class FinancialDataMasker {

    private final AccessControl accessControl;
    private final EntityManager entityManager;

    public FinancialDataMasker(AccessControl accessControl, EntityManager entityManager) {
        this.accessControl = accessControl;
        this.entityManager = entityManager;
    }

    public boolean isMasking() {
        return !accessControl.canViewFinancials();
    }

    public JobBoard mask(JobBoard board) {
        if (!isMasking()) {
            return board;
        }
        return new JobBoard(board.jobs(), maskSummaries(board.summaries()),
                maskGrouped(board.materialsByJob(), this::maskMaterials),
                maskGrouped(board.attendancesByJob(), this::maskAttendances),
                Map.of());
    }

    public JobDetail mask(JobDetail detail) {
        if (!isMasking()) {
            return detail;
        }
        return new JobDetail(detail.job(), mask(detail.summary()), maskMaterials(detail.materials()),
                maskAttendances(detail.attendances()), List.of());
    }

    public JobSummary mask(JobSummary summary) {
        return isMasking() ? summary.withoutFinancials() : summary;
    }

    public DashboardFigures mask(DashboardFigures figures) {
        return isMasking() ? figures.withoutFinancials() : figures;
    }

    public List<MaterialItem> maskMaterials(List<MaterialItem> materials) {
        if (isMasking()) {
            materials.forEach(item -> {
                detach(item);
                item.hidePurchaseInfo();
            });
        }
        return materials;
    }

    public List<Attendance> maskAttendances(List<Attendance> attendances) {
        if (isMasking()) {
            attendances.forEach(attendance -> {
                detach(attendance);
                attendance.hideWage();
                if (Hibernate.isInitialized(attendance.getEmployee())) {
                    maskEmployee(attendance.getEmployee());
                }
            });
        }
        return attendances;
    }

    public List<Employee> maskEmployees(List<Employee> employees) {
        if (isMasking()) {
            employees.forEach(this::maskEmployee);
        }
        return employees;
    }

    public Employee maskEmployee(Employee employee) {
        if (isMasking()) {
            detach(employee);
            employee.hideWage();
        }
        return employee;
    }

    private Map<Long, JobSummary> maskSummaries(Map<Long, JobSummary> summaries) {
        return summaries.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().withoutFinancials()));
    }

    private static <T> Map<Long, List<T>> maskGrouped(Map<Long, List<T>> grouped,
            java.util.function.UnaryOperator<List<T>> masker) {
        grouped.values().forEach(masker::apply);
        return grouped;
    }

    private void detach(Object entity) {
        if (entityManager.contains(entity)) {
            entityManager.detach(entity);
        }
    }
}
