package com.electrician.tracker.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Attendance;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.GroupJobBreakdown;
import com.electrician.tracker.dto.GroupJobRow;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.MaterialGroup;
import com.electrician.tracker.dto.MaterialGroupBreakdown;
import com.electrician.tracker.dto.MaterialGroupTotal;
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

    /** Material groups without cost, profit or missing purchase prices; the summary is then ordered by sale. */
    public MaterialGroupBreakdown mask(MaterialGroupBreakdown breakdown) {
        if (!isMasking()) {
            return breakdown;
        }
        return new MaterialGroupBreakdown(
                breakdown.groups().stream()
                        .map(group -> new MaterialGroup(group.total().withoutFinancials(), group.lines()))
                        .toList(),
                MaterialGroupCalculator.bySale(breakdown.summary().stream()
                        .map(MaterialGroupTotal::withoutFinancials).toList()),
                breakdown.total().withoutFinancials());
    }

    /** One group's spending per job without cost or profit; the rows are then ordered by sale. */
    public GroupJobBreakdown mask(GroupJobBreakdown breakdown) {
        if (!isMasking()) {
            return breakdown;
        }
        List<GroupJobRow> rows = breakdown.rows().stream()
                .map(GroupJobRow::withoutFinancials)
                .sorted(java.util.Comparator.comparing((GroupJobRow row) -> row.total().saleTotal()).reversed())
                .toList();
        return new GroupJobBreakdown(breakdown.groupName(), rows, breakdown.total().withoutFinancials(),
                breakdown.productIds());
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
