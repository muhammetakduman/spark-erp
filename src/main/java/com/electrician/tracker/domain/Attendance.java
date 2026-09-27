package com.electrician.tracker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** Full-day wage; the amount earned on this row is dailyWage × dayFactor. */
    @Column(name = "daily_wage", nullable = false)
    private BigDecimal dailyWage;

    /** 1.0 for a full day, 0.5 for a half day. */
    @Column(name = "day_factor", nullable = false)
    private BigDecimal dayFactor;

    private String note;

    protected Attendance() {
    }

    public Attendance(Job job, Employee employee, LocalDate attendanceDate, BigDecimal dailyWage) {
        this(job, employee, attendanceDate, dailyWage, BigDecimal.ONE, null);
    }

    public Attendance(Job job, Employee employee, LocalDate attendanceDate, BigDecimal dailyWage,
            BigDecimal dayFactor, String note) {
        this.job = job;
        this.employee = employee;
        this.attendanceDate = attendanceDate;
        this.dailyWage = dailyWage;
        this.dayFactor = dayFactor;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public BigDecimal getDailyWage() {
        return dailyWage;
    }

    public void setDailyWage(BigDecimal dailyWage) {
        this.dailyWage = dailyWage;
    }

    public BigDecimal getDayFactor() {
        return dayFactor;
    }

    public void setDayFactor(BigDecimal dayFactor) {
        this.dayFactor = dayFactor;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
