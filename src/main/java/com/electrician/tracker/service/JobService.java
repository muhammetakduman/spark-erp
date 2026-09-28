package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.util.List;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.JobDeletionImpact;
import com.electrician.tracker.repository.AttendanceRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sites and services. Anything about a site (reading, creating, changing) is
 * for an ADMIN only; a MANAGER works with services.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final MaterialItemRepository materialItemRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final AccessControl accessControl;

    public JobService(JobRepository jobRepository, MaterialItemRepository materialItemRepository,
            AttendanceRepository attendanceRepository, PaymentRepository paymentRepository,
            AccessControl accessControl) {
        this.jobRepository = jobRepository;
        this.materialItemRepository = materialItemRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.accessControl = accessControl;
    }

    /** Active jobs the logged-in user may open, e.g. to link a daily job to one (sites only for an ADMIN). */
    @Transactional(readOnly = true)
    public List<Job> findActiveAccessible() {
        return jobRepository.findByStatus(JobStatus.ACTIVE).stream()
                .filter(job -> accessControl.canAccess(job.getType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Job findById(Long id) {
        Job job = jobRepository.findWithCustomerById(id)
                .orElseThrow(() -> new NotFoundException("error.job.notFound"));
        accessControl.requireAccess(job.getType());
        return job;
    }

    @Transactional
    public Job create(Job job) {
        validate(job);
        accessControl.requireAccess(job.getType());
        return jobRepository.save(job);
    }

    @Transactional
    public Job update(Long id, Job changes) {
        validate(changes);
        accessControl.requireAccess(changes.getType());
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

    /** Counts of the records a delete would remove with the job; ADMIN only, like the delete itself. */
    @Transactional(readOnly = true)
    public JobDeletionImpact deletionImpact(Long id) {
        accessControl.requireAdmin();
        Job job = findById(id);
        return new JobDeletionImpact(id, job.getType(), JobLabels.full(job),
                materialItemRepository.countByJobId(id),
                attendanceRepository.countByJobId(id),
                paymentRepository.countByJobId(id));
    }

    /**
     * Deletes the job with its material lines, attendance and payments in one
     * transaction (the database cascades them), so nothing is left half-deleted.
     * A quote the job came from stays and loses its link.
     */
    @Transactional
    public void delete(Long id) {
        accessControl.requireAdmin();
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
