package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByJob(Long jobId) {
        return paymentRepository.findByJobIdOrderByPaymentDateAsc(jobId);
    }

    @Transactional
    public Payment addPayment(Payment payment) {
        validate(payment);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment update(Long id, Payment changes) {
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
}
