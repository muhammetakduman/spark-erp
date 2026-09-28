package com.electrician.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The user's own company, printed as the letterhead of quote and job PDFs.
 * There is only ever one row ({@link #SINGLE_ROW_ID}).
 */
@Entity
@Table(name = "company")
public class Company {

    public static final long SINGLE_ROW_ID = 1L;

    @Id
    private Long id;

    private String name;

    private String slogan;

    private String address;

    private String phone;

    private String email;

    private String web;

    @Column(name = "tax_office")
    private String taxOffice;

    @Column(name = "tax_no")
    private String taxNo;

    private byte[] logo;

    /** "#RRGGBB" used for PDF headings and table headers; {@code null} = the default brand colour. */
    @Column(name = "brand_color")
    private String brandColor;

    protected Company() {
    }

    public static Company empty() {
        Company company = new Company();
        company.id = SINGLE_ROW_ID;
        return company;
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

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public String getTaxOffice() {
        return taxOffice;
    }

    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public byte[] getLogo() {
        return logo;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }

    public String getBrandColor() {
        return brandColor;
    }

    public void setBrandColor(String brandColor) {
        this.brandColor = brandColor;
    }

    public boolean hasLogo() {
        return logo != null && logo.length > 0;
    }
}
