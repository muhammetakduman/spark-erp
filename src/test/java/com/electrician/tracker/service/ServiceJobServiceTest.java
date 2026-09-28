package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceJobServiceTest {

    private static final Long SERVICE_ID = 9L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 9, 12);

    private final Product breaker = new Product("Sigorta", ProductUnit.PIECE);
    private final Job service = new Job(new Customer("Ahmet Bey", null, null, null, null), JobType.SERVICE,
            "Pano arızası", null, SERVICE_DATE, null, JobStatus.COMPLETED, BigDecimal.valueOf(500), null, false,
            null);

    private JobService jobService;
    private MaterialService materialService;
    private ServiceJobService serviceJobService;

    @BeforeEach
    void setUp() {
        jobService = mock(JobService.class);
        materialService = mock(MaterialService.class);
        serviceJobService = new ServiceJobService(jobService, materialService);
    }

    @Test
    void newServiceGetsItsLinesWithTheServiceDate() {
        when(jobService.create(service)).thenReturn(service);
        MaterialItem draft = line(null);

        serviceJobService.save(null, service, List.of(draft));

        assertThat(draft.getJob()).isSameAs(service);
        assertThat(draft.getItemDate()).isEqualTo(SERVICE_DATE);
        verify(materialService).addItem(draft);
        verify(materialService, never()).delete(any());
    }

    @Test
    void editedServiceAddsNewUpdatesKeptAndDeletesRemovedLines() {
        when(jobService.update(SERVICE_ID, service)).thenReturn(service);
        MaterialItem kept = line(1L);
        MaterialItem removed = line(2L);
        MaterialItem added = line(null);
        when(materialService.findLineIds(SERVICE_ID)).thenReturn(List.of(1L, 2L));

        serviceJobService.save(SERVICE_ID, service, List.of(kept, added));

        verify(materialService).delete(2L);
        verify(materialService, never()).delete(1L);
        verify(materialService).update(1L, kept);
        verify(materialService).addItem(added);
        assertThat(kept.getItemDate()).isEqualTo(SERVICE_DATE);
    }

    private MaterialItem line(Long id) {
        MaterialItem item = spy(new MaterialItem(null, breaker, null, BigDecimal.ONE, null, null,
                BigDecimal.valueOf(100), PriceEntryType.UNIT, null, null, null));
        doReturn(id).when(item).getId();
        return item;
    }
}
