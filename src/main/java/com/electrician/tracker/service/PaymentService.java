package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Payments ("Tahsilat") are receivable data: only an ADMIN sees or changes them. */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccessControl accessControl;

    public PaymentService(PaymentRepository paymentRepository, AccessControl accessControl) {
        this.paymentRepository = paymentRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByJob(Long jobId) {
        accessControl.requireAdmin();
        return paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId);
    }

    @Transactional
    public Payment addPayment(Payment payment) {
        accessControl.requireAdmin();
        validate(payment);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment update(Long id, Payment changes) {
        accessControl.requireAdmin();
        validate(changes);
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.payment.notFound"));
        existing.setPaymentDate(changes.getPaymentDate());
        existing.setAmount(changes.getAmount());
        existing.setMethod(changes.getMethod());
        existing.setNote(changes.getNote());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        accessControl.requireAdmin();
        paymentRepository.deleteById(id);
    }

    private void validate(Payment payment) {
        if (payment.getJob() == null) {
            throw new ValidationException("error.payment.job.required");
        }
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("error.payment.amount.mustBePositive");
        }
        if (payment.getMethod() == null) {
            throw new ValidationException("error.payment.method.required");
        }
    }
    /** Deletes the selected payments in one statement; ADMIN only. */
    @Transactional
    public BulkDeletionResult deleteAll(Collection<Long> ids) {
        accessControl.requireAdmin();
        List<Long> distinct = ids.stream().distinct().toList();
        paymentRepository.deleteAllByIdInBatch(distinct);
        return BulkDeletionResult.allDeleted(distinct.size());
    }
}
