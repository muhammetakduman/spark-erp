package com.electrician.tracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A catalog product. The brand is a property of the product itself ("ÖZNUR"
 * cable); where it was bought belongs to each material line (supplier). The
 * same name may exist once per brand.
 */
@Entity
@Table(name = "product")
public class Product {

    private static final String BRAND_SEPARATOR = " — ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductUnit unit;

    private String brand;

    private String category;

    protected Product() {
    }

    public Product(String name, ProductUnit unit) {
        this(name, unit, null, null);
    }

    public Product(String name, ProductUnit unit, String brand, String category) {
        this.name = name;
        this.unit = unit;
        this.brand = brand;
        this.category = category;
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

    public ProductUnit getUnit() {
        return unit;
    }

    public void setUnit(ProductUnit unit) {
        this.unit = unit;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /** "ÖZNUR — 1,5mm NYA Kablo", or just the name when there is no brand. */
    public String getDisplayName() {
        return brand == null || brand.isBlank() ? name : brand + BRAND_SEPARATOR + name;
    }
}
