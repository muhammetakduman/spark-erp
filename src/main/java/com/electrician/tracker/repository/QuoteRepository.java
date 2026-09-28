package com.electrician.tracker.repository;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Quote;
import com.electrician.tracker.dto.QuoteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    /** All quotes with their lines in one query, for the list and its grand totals. */
    @Query("select distinct q from Quote q left join fetch q.items i left join fetch i.product "
            + "order by q.quoteDate desc, q.id desc")
    List<Quote> findAllWithItems();

    @Query("select q from Quote q left join fetch q.items i left join fetch i.product "
            + "left join fetch q.customer left join fetch q.job where q.id = :id")
    Optional<Quote> findWithItemsById(@Param("id") Long id);

    boolean existsByQuoteNo(String quoteNo);

    boolean existsByQuoteNoAndIdNot(String quoteNo, Long id);

    @Query("select q.quoteNo from Quote q where q.quoteNo like :prefix")
    List<String> findQuoteNosStartingWith(@Param("prefix") String prefixPattern);

    /** Which job came from which quote, for every converted quote. */
    @Query("select new com.electrician.tracker.dto.QuoteLink(q.id, q.quoteNo, q.job.id) from Quote q "
            + "where q.job is not null")
    List<QuoteLink> findJobLinks();

    @Query("select new com.electrician.tracker.dto.QuoteLink(q.id, q.quoteNo, q.job.id) from Quote q "
            + "where q.job.id = :jobId")
    Optional<QuoteLink> findJobLink(@Param("jobId") Long jobId);
}
