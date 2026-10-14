package com.electrician.tracker.repository.material;

import java.util.Collection;
import java.util.List;

import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.dto.common.IdCount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialItemRepository extends JpaRepository<MaterialItem, Long> {

    @Override
    @EntityGraph(attributePaths = { "job", "product" })
    List<MaterialItem> findAll();

    @EntityGraph(attributePaths = { "product" })
    List<MaterialItem> findByJobIdOrderByItemDateAsc(Long jobId);

    @Query("select m from MaterialItem m "
            + "join fetch m.job j "
            + "join fetch j.customer "
            + "where m.product.id = :productId "
            + "order by m.itemDate desc, m.id desc")
    List<MaterialItem> findHistoryByProductId(@Param("productId") Long productId);

    @Query("select distinct m.supplierName from MaterialItem m "
            + "where m.supplierName is not null order by m.supplierName")
    List<String> findDistinctSupplierNames();

    /** Group names used on one job's lines, earliest line first. */
    @Query("select m.groupName from MaterialItem m "
            + "where m.job.id = :jobId and m.groupName is not null order by m.id")
    List<String> findGroupNamesByJobId(@Param("jobId") Long jobId);

    @Query("select distinct m.groupName from MaterialItem m "
            + "where m.groupName is not null order by m.groupName")
    List<String> findDistinctGroupNames();

    boolean existsByGroupNameIsNull();

    /** Every line with its product, job and customer, for the per-group spending of the product screen. */
    @Query("select m from MaterialItem m join fetch m.product join fetch m.job j join fetch j.customer")
    List<MaterialItem> findAllWithProductAndJob();

    /** The given lines with their product and job, in one query. */
    @Query("select m from MaterialItem m join fetch m.product join fetch m.job where m.id in :ids")
    List<MaterialItem> findWithProductAndJobByIdIn(@Param("ids") Collection<Long> ids);

    long countByProductId(Long productId);

    long countByJobId(Long jobId);

    /** Every line with its product (no job), for the per-product purchase statistics. */
    @Query("select m from MaterialItem m join fetch m.product")
    List<MaterialItem> findAllWithProduct();
    long countByJobIdIn(Collection<Long> jobIds);

    /** Material lines per product, for many products in one query. */
    @Query("select new com.electrician.tracker.dto.common.IdCount(m.product.id, count(m)) from MaterialItem m "
            + "where m.product.id in :productIds group by m.product.id")
    List<IdCount> countByProductIds(@Param("productIds") Collection<Long> productIds);
}
