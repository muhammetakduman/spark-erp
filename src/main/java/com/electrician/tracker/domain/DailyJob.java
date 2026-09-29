package com.electrician.tracker.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One entry of the daily work plan ("Günlük İş"): where to go on a day, who
 * goes, and whether it was done. It may belong to an existing site or
 * service ({@link #job}). An entry moved to another day stays as POSTPONED
 * and the new entry points back to it ({@link #sourceDailyJobId}).
 */
@Entity
@Table(name = "daily_job")
public class DailyJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_date", nullable = false)
    private LocalDate jobDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Free-typed customer name when the customer is not in the list. */
    @Column(name = "customer_name")
    private String customerName;

    private String address;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyJobStatus status = DailyJobStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyJobPriority priority = DailyJobPriority.NORMAL;

    /** "09:00"; optional. */
    @Column(name = "time_of_day")
    private String timeOfDay;

    private String note;

    @Column(name = "completion_note")
    private String completionNote;

    @Column(name = "postponed_to")
    private LocalDate postponedTo;

    @Column(name = "source_daily_job_id")
    private Long sourceDailyJobId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "daily_job_employee",
            joinColumns = @JoinColumn(name = "daily_job_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    private Set<Employee> employees = new LinkedHashSet<>();

    protected DailyJob() {
    }

    public DailyJob(LocalDate jobDate, String title, Long createdByUserId, LocalDateTime createdAt) {
        this.jobDate = jobDate;
        this.title = title;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
    }

    /**
     * A PLANNED copy of this entry for {@code date} (same place, people and
     * notes) that remembers the entry it came from.
     */
    public DailyJob copyFor(LocalDate date, Long userId, LocalDateTime now) {
        DailyJob copy = new DailyJob(date, title, userId, now);
        copy.job = job;
        copy.customer = customer;
        copy.customerName = customerName;
        copy.address = address;
        copy.phone = phone;
        copy.priority = priority;
        copy.timeOfDay = timeOfDay;
        copy.note = note;
        copy.sourceDailyJobId = id;
        copy.employees = new LinkedHashSet<>(employees);
        return copy;
    }

    /** Marks this entry as moved to {@code date}; the caller saves the new entry. */
    public void postponeTo(LocalDate date, String reason) {
        this.status = DailyJobStatus.POSTPONED;
        this.postponedTo = date;
        this.completionNote = reason;
    }

    public void complete(String note) {
        this.status = DailyJobStatus.COMPLETED;
        this.completionNote = note;
    }

    public void markNotVisited(String reason) {
        this.status = DailyJobStatus.NOT_VISITED;
        this.completionNote = reason;
    }

    /** A job marked "not visited" is planned again (e.g. it was done after all). */
    public void reopen() {
        this.status = DailyJobStatus.PLANNED;
    }

    public void cancel() {
        this.status = DailyJobStatus.CANCELLED;
    }

    public boolean isPlanned() {
        return status == DailyJobStatus.PLANNED;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getJobDate() {
        return jobDate;
    }

    public void setJobDate(LocalDate jobDate) {
        this.jobDate = jobDate;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public DailyJobStatus getStatus() {
        return status;
    }

    public DailyJobPriority getPriority() {
        return priority;
    }

    public void setPriority(DailyJobPriority priority) {
        this.priority = priority;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public LocalDate getPostponedTo() {
        return postponedTo;
    }

    public Long getSourceDailyJobId() {
        return sourceDailyJobId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Set<Employee> getEmployees() {
        return employees;
    }

    public void replaceEmployees(Collection<Employee> newEmployees) {
        employees.clear();
        employees.addAll(newEmployees);
    }
}
