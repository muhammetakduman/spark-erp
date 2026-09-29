package com.electrician.tracker.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.IdCount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = "customer")
    List<Job> findByStatus(JobStatus status);

    long countByCustomerId(Long customerId);
    /** Jobs per customer, for many customers in one query. */
    @Query("select new com.electrician.tracker.dto.IdCount(j.customer.id, count(j)) from Job j "
            + "where j.customer.id in :customerIds group by j.customer.id")
    List<IdCount> countByCustomerIds(@Param("customerIds") Collection<Long> customerIds);
}
