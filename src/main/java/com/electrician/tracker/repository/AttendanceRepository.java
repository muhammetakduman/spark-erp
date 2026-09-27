package com.electrician.tracker.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.electrician.tracker.domain.Attendance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Override
    @EntityGraph(attributePaths = { "job", "employee" })
    List<Attendance> findAll();

    @EntityGraph(attributePaths = "employee")
    List<Attendance> findByJobIdOrderByAttendanceDateAsc(Long jobId);

    @Query("select a from Attendance a "
            + "where a.job.id = :jobId and a.attendanceDate in :dates")
    List<Attendance> findByJobAndDates(@Param("jobId") Long jobId, @Param("dates") Collection<LocalDate> dates);

    /** Rows of the given employees on the given dates that belong to any other job. */
    @Query("select a from Attendance a "
            + "join fetch a.employee "
            + "join fetch a.job j "
            + "join fetch j.customer "
            + "where a.employee.id in :employeeIds and a.attendanceDate in :dates and j.id <> :jobId "
            + "order by a.attendanceDate, a.employee.name")
    List<Attendance> findOnOtherJobs(@Param("jobId") Long jobId,
            @Param("employeeIds") Collection<Long> employeeIds, @Param("dates") Collection<LocalDate> dates);

    @Query("select a from Attendance a "
            + "join fetch a.employee "
            + "join fetch a.job j "
            + "join fetch j.customer "
            + "where a.employee.id = :employeeId and a.attendanceDate between :from and :to "
            + "order by a.attendanceDate")
    List<Attendance> findForEmployeeBetween(@Param("employeeId") Long employeeId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select a from Attendance a "
            + "join fetch a.employee "
            + "join fetch a.job j "
            + "join fetch j.customer "
            + "where a.attendanceDate between :from and :to "
            + "order by a.employee.name, a.attendanceDate")
    List<Attendance> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    long countByEmployeeId(Long employeeId);
}
