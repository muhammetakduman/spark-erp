package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<Job> findAll() {
        return jobRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Job> findByType(JobType type) {
        return jobRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public List<Job> findByTypeAndStatus(JobType type, JobStatus status) {
        return jobRepository.findByTypeAndStatus(type, status);
    }

    @Transactional(readOnly = true)
    public Job findById(Long id) {
        return jobRepository.findWithCustomerById(id)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
    }

    @Transactional
    public Job create(Job job) {
        validate(job);
        return jobRepository.save(job);
    }

    @Transactional
    public Job update(Long id, Job changes) {
        validate(changes);
        Job existing = findById(id);
        existing.setCustomer(changes.getCustomer());
        existing.setType(changes.getType());
        existing.setName(changes.getName());
        existing.setAddress(changes.getAddress());
        existing.setStartDate(changes.getStartDate());
        existing.setEndDate(changes.getEndDate());
        existing.setStatus(changes.getStatus());
        existing.setServiceFee(changes.getServiceFee());
        existing.setLaborFee(changes.getLaborFee());
        existing.setServiceFeeVat(changes.getServiceFeeVatRate(), changes.getServiceFeeVatIncluded());
        existing.setLaborFeeVat(changes.getLaborFeeVatRate(), changes.getLaborFeeVatIncluded());
        existing.setPaymentReceived(changes.isPaymentReceived());
        existing.setDescription(changes.getDescription());
        return existing;
    }

    @Transactional
    public void setPaymentReceived(Long id, boolean received) {
        findById(id).setPaymentReceived(received);
    }

    @Transactional
    public void changeStatus(Long id, JobStatus status) {
        findById(id).setStatus(status);
    }

    @Transactional
    public void delete(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new NotFoundException("error.job.notFound");
        }
        jobRepository.deleteById(id);
    }

    private void validate(Job job) {
        if (job.getCustomer() == null) {
            throw new ValidationException("error.job.customer.required");
        }
        if (job.getType() == null) {
            throw new ValidationException("error.job.type.required");
        }
        if (job.getStatus() == null) {
            throw new ValidationException("error.job.status.required");
        }
        if (job.getStartDate() != null && job.getEndDate() != null && job.getEndDate().isBefore(job.getStartDate())) {
            throw new ValidationException("error.job.endBeforeStart");
        }
        requireNonNegative(job.getServiceFee(), "error.job.serviceFee.negative");
        requireNonNegative(job.getLaborFee(), "error.job.laborFee.negative");
        VatRules.validate(job.getServiceFeeVatRate(), job.getServiceFeeVatIncluded());
        VatRules.validate(job.getLaborFeeVatRate(), job.getLaborFeeVatIncluded());
        job.setServiceFeeVat(job.getServiceFeeVatRate(), VatRules.includedOrNull(job.getServiceFeeVatRate(),
                job.getServiceFeeVatIncluded()));
        job.setLaborFeeVat(job.getLaborFeeVatRate(), VatRules.includedOrNull(job.getLaborFeeVatRate(),
                job.getLaborFeeVatIncluded()));
    }

    private void requireNonNegative(BigDecimal value, String messageKey) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(messageKey);
        }
    }
}
