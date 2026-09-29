package com.electrician.tracker.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.DailyJob;
import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.dto.DailyJobLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyJobRepository extends JpaRepository<DailyJob, Long> {

    /** Every entry of the given days with its people, customer and linked job in one query. */
    @Query("select distinct d from DailyJob d "
            + "left join fetch d.employees "
            + "left join fetch d.customer "
            + "left join fetch d.job j "
            + "left join fetch j.customer "
            + "where d.jobDate between :from and :to")
    List<DailyJob> findPlanBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Entries before {@code today} still in one of {@code statuses} (forgotten or not visited). */
    @Query("select distinct d from DailyJob d "
            + "left join fetch d.employees "
            + "left join fetch d.customer "
            + "left join fetch d.job j "
            + "left join fetch j.customer "
            + "where d.jobDate < :today and d.status in :statuses")
    List<DailyJob> findOverdue(@Param("today") LocalDate today,
            @Param("statuses") Collection<DailyJobStatus> statuses);

    /** Entries of {@code date} in {@code status} that were moved there from an earlier day. */
    @Query("select distinct d from DailyJob d "
            + "left join fetch d.employees "
            + "left join fetch d.customer "
            + "left join fetch d.job j "
            + "left join fetch j.customer "
            + "where d.jobDate = :date and d.status = :status and d.sourceDailyJobId is not null")
    List<DailyJob> findPostponedInto(@Param("date") LocalDate date, @Param("status") DailyJobStatus status);

    /** Several entries with their details in one query (bulk actions). */
    @Query("select distinct d from DailyJob d "
            + "left join fetch d.employees "
            + "left join fetch d.customer "
            + "left join fetch d.job "
            + "where d.id in :ids")
    List<DailyJob> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select count(d) from DailyJob d where d.jobDate < :today and d.status in :statuses")
    long countOverdue(@Param("today") LocalDate today, @Param("statuses") Collection<DailyJobStatus> statuses);

    @Query("select d from DailyJob d "
            + "left join fetch d.employees "
            + "left join fetch d.customer "
            + "left join fetch d.job j "
            + "left join fetch j.customer "
            + "where d.id = :id")
    Optional<DailyJob> findWithDetailsById(@Param("id") Long id);

    /** "Moved from" links of every postponed-and-recreated entry, to count postponements in memory. */
    @Query("select new com.electrician.tracker.dto.DailyJobLink(d.id, d.sourceDailyJobId) from DailyJob d "
            + "where d.sourceDailyJobId is not null")
    List<DailyJobLink> findSourceLinks();

    /**
     * Planned entries on {@code date} of any of {@code employeeIds} other than
     * {@code excludedId}; only the matching employees are fetched.
     */
    @Query("select distinct d from DailyJob d join fetch d.employees e "
            + "where d.jobDate = :date and d.status = :status and e.id in :employeeIds and d.id <> :excludedId")
    List<DailyJob> findBookings(@Param("date") LocalDate date, @Param("status") DailyJobStatus status,
            @Param("employeeIds") Collection<Long> employeeIds, @Param("excludedId") Long excludedId);
}
