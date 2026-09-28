package com.electrician.tracker.repository;

import com.electrician.tracker.domain.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {

    long countByProductId(Long productId);
}
