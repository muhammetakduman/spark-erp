package com.electrician.tracker.repository;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Override
    @EntityGraph(attributePaths = "customer")
    List<Job> findAll();

    @EntityGraph(attributePaths = "customer")
    Optional<Job> findWithCustomerById(Long id);

    @EntityGraph(attributePaths = "customer")
    List<Job> findByType(JobType type);

    @EntityGraph(attributePaths = "customer")
    List<Job> findByTypeAndStatus(JobType type, JobStatus status);

    long countByCustomerId(Long customerId);
}
