package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.JobType;

/**
 * One time a product was given to a customer: "to whom, when, at what price".
 * {@code saleTotal} is the line total as entered (VAT treatment per
 * {@code vatRate}/{@code vatIncluded}); purchase prices carry their own VAT.
 * {@code purchaseUnitPrice} is always the lira value (so purchases made on
 * different days and in different currencies compare); a dollar or euro
 * purchase also keeps its original amount and rate for display.
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
        BigDecimal saleTotal,
        CurrencyCode purchaseCurrency,
        BigDecimal purchaseOriginalUnitPrice,
        BigDecimal purchaseExchangeRate) {

    /** Bought in dollars or euros. */
    public boolean isForeignCurrencyPurchase() {
        return purchaseCurrency != null && purchaseCurrency.isForeign() && purchaseOriginalUnitPrice != null;
    }

    /** Sale side only; the purchase price, its currency, its VAT and the supplier are removed. */
    public ProductUsageRow withoutPurchaseInfo() {
        return new ProductUsageRow(jobId, jobType, date, customerName, jobName, quantity, null, null, null, null,
                null, saleUnitPrice, vatRate, vatIncluded, saleTotal, null, null, null);
    }
}
