package com.electrician.tracker.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_daily_wage", nullable = false)
    private BigDecimal defaultDailyWage;

    @Column(name = "is_master", nullable = false)
    private boolean master;

    @Column(nullable = false)
    private boolean active;

    protected Employee() {
    }

    public Employee(String name, BigDecimal defaultDailyWage, boolean master, boolean active) {
        this.name = name;
        this.defaultDailyWage = defaultDailyWage;
        this.master = master;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDefaultDailyWage() {
        return defaultDailyWage;
    }

    public void setDefaultDailyWage(BigDecimal defaultDailyWage) {
        this.defaultDailyWage = defaultDailyWage;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
