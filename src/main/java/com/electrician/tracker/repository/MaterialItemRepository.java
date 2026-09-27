package com.electrician.tracker.repository;

import java.util.List;

import com.electrician.tracker.domain.MaterialItem;
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

    long countByProductId(Long productId);
}
