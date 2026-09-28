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
@Table(name = "material_item")
public class MaterialItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "item_date")
    private LocalDate itemDate;

    @Column(nullable = false)
    private BigDecimal quantity;

    /** The purchase unit price as entered, in {@link #purchaseCurrency}. */
    @Column(name = "purchase_unit_price")
    private BigDecimal purchaseUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_currency", nullable = false)
    private CurrencyCode purchaseCurrency = CurrencyCode.TRY;

    /** Lira value of one unit of the purchase currency on the purchase day (1 for lira). */
    @Column(name = "purchase_exchange_rate")
    private BigDecimal purchaseExchangeRate;

    /** Purchase unit price in lira, frozen when the line is saved; every cost uses this value. */
    @Column(name = "purchase_unit_price_tl")
    private BigDecimal purchaseUnitPriceTl;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "sale_unit_price", nullable = false)
    private BigDecimal saleUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_entry_type", nullable = false)
    private PriceEntryType priceEntryType;

    @Column(name = "sale_total_amount")
    private BigDecimal saleTotalAmount;

    @Column(name = "vat_rate")
    private Integer vatRate;

    @Column(name = "vat_included")
    private Boolean vatIncluded;

    /** VAT of the purchase price, independent of the sale VAT ({@code null} = none). */
    @Column(name = "purchase_vat_rate")
    private Integer purchaseVatRate;

    @Column(name = "purchase_vat_included")
    private Boolean purchaseVatIncluded;

    private String note;

    protected MaterialItem() {
    }

    public MaterialItem(Job job, Product product, LocalDate itemDate, BigDecimal quantity,
            BigDecimal purchaseUnitPrice, String supplierName, BigDecimal saleUnitPrice,
            PriceEntryType priceEntryType, BigDecimal saleTotalAmount, Integer vatRate, String note) {
        this.job = job;
        this.product = product;
        this.itemDate = itemDate;
        this.quantity = quantity;
        this.purchaseUnitPrice = purchaseUnitPrice;
        this.supplierName = supplierName;
        this.saleUnitPrice = saleUnitPrice;
        this.priceEntryType = priceEntryType;
        this.saleTotalAmount = saleTotalAmount;
        this.vatRate = vatRate;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }

    public void setItemDate(LocalDate itemDate) {
        this.itemDate = itemDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchaseUnitPrice() {
        return purchaseUnitPrice;
    }

    public void setPurchaseUnitPrice(BigDecimal purchaseUnitPrice) {
        this.purchaseUnitPrice = purchaseUnitPrice;
    }

    public CurrencyCode getPurchaseCurrency() {
        return purchaseCurrency;
    }

    public BigDecimal getPurchaseExchangeRate() {
        return purchaseExchangeRate;
    }

    /** The lira purchase unit price; a lira price not yet normalized by the service is its own lira value. */
    public BigDecimal getPurchaseUnitPriceTl() {
        if (purchaseUnitPriceTl == null && purchaseUnitPrice != null
                && (purchaseCurrency == null || !purchaseCurrency.isForeign())) {
            return purchaseUnitPrice;
        }
        return purchaseUnitPriceTl;
    }

    /** Currency and exchange rate of the entered purchase price, and the lira value derived from them. */
    public void setPurchaseCurrency(CurrencyCode currency, BigDecimal exchangeRate, BigDecimal unitPriceTl) {
        this.purchaseCurrency = currency;
        this.purchaseExchangeRate = exchangeRate;
        this.purchaseUnitPriceTl = unitPriceTl;
    }

    /** Bought in dollars or euros (with a purchase price). */
    public boolean isForeignCurrencyPurchase() {
        return purchaseUnitPrice != null && purchaseCurrency != null && purchaseCurrency.isForeign();
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getSaleUnitPrice() {
        return saleUnitPrice;
    }

    public void setSaleUnitPrice(BigDecimal saleUnitPrice) {
        this.saleUnitPrice = saleUnitPrice;
    }

    public PriceEntryType getPriceEntryType() {
        return priceEntryType;
    }

    public void setPriceEntryType(PriceEntryType priceEntryType) {
        this.priceEntryType = priceEntryType;
    }

    public BigDecimal getSaleTotalAmount() {
        return saleTotalAmount;
    }

    public void setSaleTotalAmount(BigDecimal saleTotalAmount) {
        this.saleTotalAmount = saleTotalAmount;
    }

    public Integer getVatRate() {
        return vatRate;
    }

    public void setVatRate(Integer vatRate) {
        this.vatRate = vatRate;
    }

    public Boolean getVatIncluded() {
        return vatIncluded;
    }

    public void setVatIncluded(Boolean vatIncluded) {
        this.vatIncluded = vatIncluded;
    }

    public Integer getPurchaseVatRate() {
        return purchaseVatRate;
    }

    public Boolean getPurchaseVatIncluded() {
        return purchaseVatIncluded;
    }

    public void setPurchaseVat(Integer rate, Boolean included) {
        this.purchaseVatRate = rate;
        this.purchaseVatIncluded = included;
    }

    public String getNote() {
        return note;
    }

    /**
     * Drops the purchase price, its VAT and the supplier from this (detached)
     * instance, for users who must not see purchase information.
     */
    public void hidePurchaseInfo() {
        this.purchaseUnitPrice = null;
        this.purchaseCurrency = null;
        this.purchaseExchangeRate = null;
        this.purchaseUnitPriceTl = null;
        this.supplierName = null;
        this.purchaseVatRate = null;
        this.purchaseVatIncluded = null;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
