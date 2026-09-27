package com.electrician.tracker.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.MaterialItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves a service job together with its material lines in one transaction,
 * for both a new service and one reopened for editing. Material lines of a
 * service always carry the service date.
 */
@Service
public class ServiceJobService {

    private final JobService jobService;
    private final MaterialService materialService;

    public ServiceJobService(JobService jobService, MaterialService materialService) {
        this.jobService = jobService;
        this.materialService = materialService;
    }

    /**
     * Creates ({@code id == null}) or updates the service, then makes its
     * material lines match {@code lines}: lines without an id are added,
     * saved lines missing from {@code lines} are deleted, the rest are updated.
     */
    @Transactional
    public Job save(Long id, Job service, List<MaterialItem> lines) {
        Job saved = id == null ? jobService.create(service) : jobService.update(id, service);
        if (id != null) {
            deleteRemovedLines(id, lines);
        }
        for (MaterialItem line : lines) {
            line.setJob(saved);
            line.setItemDate(saved.getStartDate());
            if (line.getId() == null) {
                materialService.addItem(line);
            } else {
                materialService.update(line.getId(), line);
            }
        }
        return saved;
    }

    private void deleteRemovedLines(Long jobId, List<MaterialItem> keptLines) {
        Set<Long> keptIds = keptLines.stream()
                .map(MaterialItem::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        materialService.findByJob(jobId).stream()
                .map(MaterialItem::getId)
                .filter(lineId -> !keptIds.contains(lineId))
                .forEach(materialService::delete);
    }
}
