package com.electrician.tracker.service;

import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Quote;
import com.electrician.tracker.domain.QuoteItem;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteRow;
import com.electrician.tracker.dto.QuoteTotals;
import com.electrician.tracker.dto.QuoteView;

/** Turns quote entities into what the list, the editor and the PDF use. */
final class QuoteMapper {

    private QuoteMapper() {
    }

    static QuoteView toView(Quote quote) {
        QuoteDraft draft = toDraft(quote);
        return new QuoteView(quote.getId(), draft, quote.getPreparedByName(), quote.getPreparedByTitle(),
                quote.getJob() == null ? null : quote.getJob().getId(),
                quote.getJob() == null ? null : quote.getJob().getType(), totals(draft));
    }

    static QuoteDraft toDraft(Quote quote) {
        return new QuoteDraft(quote.getQuoteNo(), quote.getQuoteDate(), quote.getValidityDays(),
                quote.getCustomer() == null ? null : quote.getCustomer().getId(), quote.getCompanyName(),
                quote.getAddress(), quote.getContactPerson(), quote.getPhone(), quote.getFax(), quote.getEmail(),
                quote.getSubject(), quote.getDiscountType(), quote.getDiscountValue(), quote.getLaborAmount(),
                quote.getVatRate(), quote.getNotes(), quote.getStatus(), lines(quote.getItems()), false,
                quote.getPreparedByName(), quote.getPreparedByTitle());
    }

    static QuoteRow toRow(Quote quote, LocalDate today) {
        LocalDate validUntil = quote.getQuoteDate().plusDays(quote.getValidityDays());
        boolean expired = quote.getStatus() == QuoteStatus.SENT && validUntil.isBefore(today);
        QuoteTotals totals = totals(toDraft(quote));
        return new QuoteRow(quote.getId(), quote.getQuoteNo(), quote.getQuoteDate(), quote.getCompanyName(),
                quote.getSubject(), totals.grandTotal(), quote.getStatus(), validUntil, expired,
                quote.getJob() == null ? null : quote.getJob().getId());
    }

    static QuoteTotals totals(QuoteDraft draft) {
        return QuoteCalculator.calculate(draft.lines(), draft.discountType(), draft.discountValue(),
                draft.laborAmount(), draft.vatRate());
    }

    private static List<QuoteLine> lines(List<QuoteItem> items) {
        return items.stream()
                .map(item -> new QuoteLine(item.getLineNo(), item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductName(), item.getBrand(), item.getQuantity(), item.getUnit(),
                        item.getUnitPrice(), item.getDescription()))
                .toList();
    }
}
