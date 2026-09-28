package com.electrician.tracker.domain;

import java.math.BigDecimal;

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

/**
 * One line of a {@link Quote}: a catalog product or a free text item, with a
 * VAT-exclusive unit price typed by the user.
 */
@Entity
@Table(name = "quote_item")
public class QuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "free_product_name")
    private String freeProductName;

    private String brand;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductUnit unit;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    private String description;

    protected QuoteItem() {
    }

    public QuoteItem(Product product, String freeProductName, String brand, BigDecimal quantity, ProductUnit unit,
            BigDecimal unitPrice, String description) {
        this.product = product;
        this.freeProductName = freeProductName;
        this.brand = brand;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.description = description;
    }

    void attachTo(Quote quote, int lineNo) {
        this.quote = quote;
        this.lineNo = lineNo;
    }

    public Long getId() {
        return id;
    }

    public Quote getQuote() {
        return quote;
    }

    public int getLineNo() {
        return lineNo;
    }

    public Product getProduct() {
        return product;
    }

    public String getFreeProductName() {
        return freeProductName;
    }

    /** The catalog product's name, or the free text of a free item. */
    public String getProductName() {
        return product == null ? freeProductName : product.getName();
    }

    public String getBrand() {
        return brand;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public ProductUnit getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getDescription() {
        return description;
    }
}
