package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.EmployeeRepository;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private static final Long EMPLOYEE_ID = 5L;

    private EmployeeRepository employeeRepository;
    private AttendanceRepository attendanceRepository;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        attendanceRepository = mock(AttendanceRepository.class);
        service = new EmployeeService(employeeRepository, attendanceRepository, TestAccess.admin(),
                TestAccess.masker(TestAccess.admin()));
    }

    @Test
    void refusesToDeleteEmployeeWithAttendanceAndReportsTheCount() {
        when(attendanceRepository.countByEmployeeId(EMPLOYEE_ID)).thenReturn(12L);

        assertThatThrownBy(() -> service.delete(EMPLOYEE_ID))
                .isInstanceOfSatisfying(ReferencedEntityException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("error.employee.delete.hasAttendance");
                    assertThat(ex.getReferenceCount()).isEqualTo(12L);
                });
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    void deletesEmployeeWithoutAttendance() {
        when(attendanceRepository.countByEmployeeId(EMPLOYEE_ID)).thenReturn(0L);

        service.delete(EMPLOYEE_ID);

        verify(employeeRepository).deleteById(EMPLOYEE_ID);
    }
}
