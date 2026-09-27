package com.electrician.tracker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    private String name;

    private String address;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "service_fee")
    private BigDecimal serviceFee;

    @Column(name = "labor_fee")
    private BigDecimal laborFee;

    @Column(name = "service_fee_vat_rate")
    private Integer serviceFeeVatRate;

    @Column(name = "service_fee_vat_included")
    private Boolean serviceFeeVatIncluded;

    @Column(name = "labor_fee_vat_rate")
    private Integer laborFeeVatRate;

    @Column(name = "labor_fee_vat_included")
    private Boolean laborFeeVatIncluded;

    @Column(name = "payment_received", nullable = false)
    private boolean paymentReceived;

    private String description;

    protected Job() {
    }

    public Job(Customer customer, JobType type, String name, String address, LocalDate startDate,
            LocalDate endDate, JobStatus status, BigDecimal serviceFee, BigDecimal laborFee,
            boolean paymentReceived, String description) {
        this.customer = customer;
        this.type = type;
        this.name = name;
        this.address = address;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.serviceFee = serviceFee;
        this.laborFee = laborFee;
        this.paymentReceived = paymentReceived;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getLaborFee() {
        return laborFee;
    }

    public void setLaborFee(BigDecimal laborFee) {
        this.laborFee = laborFee;
    }

    public Integer getServiceFeeVatRate() {
        return serviceFeeVatRate;
    }

    public Boolean getServiceFeeVatIncluded() {
        return serviceFeeVatIncluded;
    }

    public void setServiceFeeVat(Integer rate, Boolean included) {
        this.serviceFeeVatRate = rate;
        this.serviceFeeVatIncluded = included;
    }

    public Integer getLaborFeeVatRate() {
        return laborFeeVatRate;
    }

    public Boolean getLaborFeeVatIncluded() {
        return laborFeeVatIncluded;
    }

    public void setLaborFeeVat(Integer rate, Boolean included) {
        this.laborFeeVatRate = rate;
        this.laborFeeVatIncluded = included;
    }

    public boolean isPaymentReceived() {
        return paymentReceived;
    }

    public void setPaymentReceived(boolean paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
