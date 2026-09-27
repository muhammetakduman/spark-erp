package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.electrician.tracker.domain.JobType;

/**
 * One time a product was given to a customer: "to whom, when, at what price".
 * {@code saleTotal} is the line total as entered (VAT treatment per
 * {@code vatRate}/{@code vatIncluded}); purchase prices carry their own VAT.
 */
public record ProductUsageRow(
        Long jobId,
        JobType jobType,
        LocalDate date,
        String customerName,
        String jobName,
        BigDecimal quantity,
        BigDecimal purchaseUnitPrice,
        Integer purchaseVatRate,
        Boolean purchaseVatIncluded,
        BigDecimal purchaseUnitPriceExcludingVat,
        String supplierName,
        BigDecimal saleUnitPrice,
        Integer vatRate,
        Boolean vatIncluded,
        BigDecimal saleTotal) {
}
