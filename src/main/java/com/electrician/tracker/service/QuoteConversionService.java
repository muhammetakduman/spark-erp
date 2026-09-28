package com.electrician.tracker.service;

import java.time.LocalDate;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.Quote;
import com.electrician.tracker.domain.QuoteItem;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "İşe Dönüştür": turns an accepted quote into a site or service job in one
 * transaction. Customer, name (the quote subject) and date carry over; each
 * line becomes a material line with its quantity, VAT-exclusive sale price,
 * the quote's VAT rate and its product (a free item is added to the catalog
 * first). Purchase price and supplier stay empty for the real purchase. The
 * quote's labor becomes the job's labor fee; the job is linked to the quote.
 * ADMIN only.
 */
@Service
public class QuoteConversionService {

    private final QuoteService quoteService;
    private final CustomerService customerService;
    private final JobService jobService;
    private final MaterialService materialService;
    private final ProductService productService;
    private final AccessControl accessControl;

    public QuoteConversionService(QuoteService quoteService, CustomerService customerService, JobService jobService,
            MaterialService materialService, ProductService productService, AccessControl accessControl) {
        this.quoteService = quoteService;
        this.customerService = customerService;
        this.jobService = jobService;
        this.materialService = materialService;
        this.productService = productService;
        this.accessControl = accessControl;
    }

    @Transactional
    public Job convertToJob(Long quoteId, JobType type) {
        accessControl.requireAdmin();
        if (type == null) {
            throw new ValidationException("error.job.type.required");
        }
        Quote quote = quoteService.findEntity(quoteId);
        if (quote.getStatus() != QuoteStatus.ACCEPTED) {
            throw new ValidationException("error.quote.convert.notAccepted");
        }
        if (quote.isConvertedToJob()) {
            throw new ValidationException("error.quote.convert.alreadyConverted");
        }
        Job job = jobService.create(newJob(quote, type));
        for (QuoteItem item : quote.getItems()) {
            materialService.addItem(toMaterial(job, item, quote.getVatRate()));
        }
        quote.setJob(job);
        return job;
    }

    private Job newJob(Quote quote, JobType type) {
        LocalDate date = quote.getQuoteDate();
        JobStatus status = type == JobType.SITE ? JobStatus.ACTIVE : JobStatus.COMPLETED;
        String name = quote.getSubject() == null ? quote.getQuoteNo() : quote.getSubject();
        Job job = new Job(customerOf(quote), type, name, quote.getAddress(), date, null, status, null,
                quote.getLaborAmount(), false, null);
        job.setLaborFeeVat(quote.getVatRate(), quote.getVatRate() == null ? null : Boolean.FALSE);
        return job;
    }

    /** The linked customer, the one with the same name, or a new one made from the quote's customer block. */
    private Customer customerOf(Quote quote) {
        if (quote.getCustomer() != null) {
            return quote.getCustomer();
        }
        return customerService.findOrCreate(quote.getCompanyName(), quote.getPhone(), quote.getAddress(),
                quote.getEmail()).customer();
    }

    private MaterialItem toMaterial(Job job, QuoteItem item, Integer vatRate) {
        MaterialItem material = new MaterialItem(job, productOf(item), job.getStartDate(), item.getQuantity(), null,
                null, item.getUnitPrice(), PriceEntryType.UNIT, null, vatRate, item.getDescription());
        material.setVatIncluded(vatRate == null ? null : Boolean.FALSE);
        return material;
    }

    private Product productOf(QuoteItem item) {
        if (item.getProduct() != null) {
            return item.getProduct();
        }
        return productService.createOrReuse(item.getFreeProductName(), item.getBrand(), null, item.getUnit())
                .product();
    }
}
