package com.electrician.tracker.repository;

import java.util.Collection;
import java.util.List;

import com.electrician.tracker.domain.QuoteItem;
import com.electrician.tracker.dto.IdCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {

    long countByProductId(Long productId);
    long countByQuoteIdIn(Collection<Long> quoteIds);

    /** Quote lines per product, for many products in one query. */
    @Query("select new com.electrician.tracker.dto.IdCount(i.product.id, count(i)) from QuoteItem i "
            + "where i.product.id in :productIds group by i.product.id")
    List<IdCount> countByProductIds(@Param("productIds") Collection<Long> productIds);
}
