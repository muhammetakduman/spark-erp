package com.electrician.tracker.repository.customer;

import com.electrician.tracker.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
