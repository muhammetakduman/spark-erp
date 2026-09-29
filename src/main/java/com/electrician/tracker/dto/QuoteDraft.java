package com.electrician.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.QuoteStatus;

/**
 * A quote as the editor sends it to be saved: customer block, numbering,
 * lines in their order, discount, labor, VAT rate ({@code null} = no VAT),
 * notes and status. Without a {@code customerId} the typed company name is
 * added to the customer list when {@code addCustomerToList} is set (or
 * linked to the customer that already has that name); otherwise the quote is
 * kept without a customer record. "Hazırlayan" / "Unvan" are free texts;
 * blank ones are filled with the logged-in user's name and title on save.
 */
public record QuoteDraft(
        String quoteNo,
        LocalDate quoteDate,
        int validityDays,
        Long customerId,
        String companyName,
        String address,
        String contactPerson,
        String phone,
        String fax,
        String email,
        String subject,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal laborAmount,
        Integer vatRate,
        String notes,
        QuoteStatus status,
        List<QuoteLine> lines,
        boolean addCustomerToList,
        String preparedByName,
        String preparedByTitle) {
}
