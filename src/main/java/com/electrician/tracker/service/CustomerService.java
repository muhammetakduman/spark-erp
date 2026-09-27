package com.electrician.tracker.service;

import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.repository.CustomerRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;

    public CustomerService(CustomerRepository customerRepository, JobRepository jobRepository) {
        this.customerRepository = customerRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("error.customer.notFound"));
    }

    @Transactional
    public Customer create(Customer customer) {
        validate(customer);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer changes) {
        validate(changes);
        Customer existing = findById(id);
        existing.setName(changes.getName());
        existing.setPhone(changes.getPhone());
        existing.setAddress(changes.getAddress());
        existing.setTaxNo(changes.getTaxNo());
        existing.setNote(changes.getNote());
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        long jobCount = jobRepository.countByCustomerId(id);
        if (jobCount > 0) {
            throw new ReferencedEntityException("error.customer.delete.hasJobs", jobCount);
        }
        customerRepository.deleteById(id);
    }

    private void validate(Customer customer) {
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new ValidationException("error.customer.name.required");
        }
    }
}
