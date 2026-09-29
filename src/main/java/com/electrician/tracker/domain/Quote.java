package com.electrician.tracker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A written price quote ("Teklif") given to a customer. Item prices are
 * VAT-exclusive; discount, labor and VAT are applied on the items total.
 * Once accepted it can be turned into a job, which is then linked here.
 */
@Entity
@Table(name = "quote")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_no", nullable = false)
    private String quoteNo;

    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String address;

    @Column(name = "contact_person")
    private String contactPerson;

    private String phone;

    private String fax;

    private String email;

    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType = DiscountType.NONE;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "labor_amount")
    private BigDecimal laborAmount;

    @Column(name = "vat_rate")
    private Integer vatRate;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "prepared_by_name")
    private String preparedByName;

    @Column(name = "prepared_by_title")
    private String preparedByTitle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<QuoteItem> items = new ArrayList<>();

    protected Quote() {
    }

    public Quote(String preparedByName, String preparedByTitle, LocalDateTime createdAt) {
        this.preparedByName = preparedByName;
        this.preparedByTitle = preparedByTitle;
        this.createdAt = createdAt;
    }

    /** Replaces all lines, numbering them 1..n in the given order. */
    public void replaceItems(List<QuoteItem> newItems) {
        items.clear();
        int lineNo = 1;
        for (QuoteItem item : newItems) {
            item.attachTo(this, lineNo++);
            items.add(item);
        }
    }

    public boolean isConvertedToJob() {
        return job != null;
    }

    public Long getId() {
        return id;
    }

    public String getQuoteNo() {
        return quoteNo;
    }

    public void setQuoteNo(String quoteNo) {
        this.quoteNo = quoteNo;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(LocalDate quoteDate) {
        this.quoteDate = quoteDate;
    }

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscount(DiscountType type, BigDecimal value) {
        this.discountType = type;
        this.discountValue = value;
    }

    public BigDecimal getLaborAmount() {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount) {
        this.laborAmount = laborAmount;
    }

    public Integer getVatRate() {
        return vatRate;
    }

    public void setVatRate(Integer vatRate) {
        this.vatRate = vatRate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
    }

    /** Free texts typed on the quote; not linked to a user account. */
    public void setPreparedBy(String name, String title) {
        this.preparedByName = name;
        this.preparedByTitle = title;
    }

    public String getPreparedByName() {
        return preparedByName;
    }

    public String getPreparedByTitle() {
        return preparedByTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public List<QuoteItem> getItems() {
        return items;
    }
}
