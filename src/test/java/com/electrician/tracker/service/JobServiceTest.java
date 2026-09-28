package com.electrician.tracker.service;

import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.repository.MaterialItemRepository;
import com.electrician.tracker.repository.AttendanceRepository;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobServiceTest {

    private final Customer customer = new Customer("Ahmet Bey", null, null, null, null);

    private JobRepository repository;
    private JobService service;

    @BeforeEach
    void setUp() {
        repository = mock(JobRepository.class);
        when(repository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new JobService(repository, mock(MaterialItemRepository.class), mock(AttendanceRepository.class),
                mock(PaymentRepository.class), TestAccess.admin());
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        Job job = site(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9));

        assertThatThrownBy(() -> service.create(job))
                .isInstanceOf(ValidationException.class)
                .hasMessage("error.job.endBeforeStart");
        verify(repository, never()).save(any());
    }

    @Test
    void acceptsSameDayAndOpenEndedJobs() {
        service.create(site(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10)));
        service.create(site(LocalDate.of(2026, 9, 10), null));
    }

    private Job site(LocalDate start, LocalDate end) {
        return new Job(customer, JobType.SITE, "Blok B", null, start, end, JobStatus.ACTIVE, null, null, false, null);
    }
}
