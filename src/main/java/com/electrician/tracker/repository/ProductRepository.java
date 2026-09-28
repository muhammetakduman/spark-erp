package com.electrician.tracker.repository;

import java.util.List;

import com.electrician.tracker.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select distinct p.brand from Product p where p.brand is not null order by p.brand")
    List<String> findDistinctBrands();

    @Query("select distinct p.category from Product p where p.category is not null order by p.category")
    List<String> findDistinctCategories();
}
