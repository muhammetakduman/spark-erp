package com.electrician.tracker.repository;

import java.util.Collection;
import java.util.List;

import com.electrician.tracker.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByJobIdOrderByPaymentDateAsc(Long jobId);

    long countByJobId(Long jobId);
    long countByJobIdIn(Collection<Long> jobIds);
}
