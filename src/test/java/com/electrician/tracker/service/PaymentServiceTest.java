package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.repository.PaymentRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private static final Long PAYMENT_ID = 3L;

    private final Job service = new Job(new Customer("Ahmet Bey", null, null, null, null), JobType.SERVICE,
            "Pano arızası", null, LocalDate.of(2026, 9, 1), null, JobStatus.COMPLETED, null, null, false, null);

    private PaymentRepository repository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentRepository.class);
        paymentService = new PaymentService(repository, TestAccess.admin());
    }

    @Test
    void updatesAMistypedPayment() {
        Payment existing = new Payment(service, LocalDate.of(2026, 9, 1), new BigDecimal("5000"),
                PaymentMethod.CASH, null);
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.of(existing));

        paymentService.update(PAYMENT_ID, new Payment(service, LocalDate.of(2026, 9, 2), new BigDecimal("500"),
                PaymentMethod.TRANSFER, "EFT"));

        assertThat(existing.getAmount()).isEqualByComparingTo("500");
        assertThat(existing.getPaymentDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(existing.getMethod()).isEqualTo(PaymentMethod.TRANSFER);
        assertThat(existing.getNote()).isEqualTo("EFT");
    }

    @Test
    void rejectsNonPositiveAmountOnUpdate() {
        Payment changes = new Payment(service, LocalDate.of(2026, 9, 2), BigDecimal.ZERO, PaymentMethod.CASH, null);

        assertThatThrownBy(() -> paymentService.update(PAYMENT_ID, changes))
                .hasMessage("error.payment.amount.mustBePositive");
    }

    @Test
    void reportsMissingPayment() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.empty());
        Payment changes = new Payment(service, LocalDate.of(2026, 9, 2), BigDecimal.ONE, PaymentMethod.CASH, null);

        assertThatThrownBy(() -> paymentService.update(PAYMENT_ID, changes)).isInstanceOf(NotFoundException.class);
    }
}
