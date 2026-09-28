package com.electrician.tracker.service;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.repository.CustomerRepository;
import com.electrician.tracker.repository.JobRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customers. Names are matched the way people type them: "ahmet yılmaz" and
 * "AHMET YILMAZ " are the same customer ({@link MetinKarsilastirici}), so a
 * customer added on the fly (e.g. from a quote) is never created twice.
 */
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

    /** The customer whose name is the same as {@code name} apart from case, Turkish letters and spaces. */
    @Transactional(readOnly = true)
    public Optional<Customer> findByName(String name) {
        return findByName(customerRepository.findAll(), name);
    }

    /** Pure lookup over an already-loaded list (the customer list is small). */
    public static Optional<Customer> findByName(List<Customer> customers, String name) {
        String wanted = MetinKarsilastirici.normalize(name);
        if (wanted.isEmpty()) {
            return Optional.empty();
        }
        return customers.stream()
                .filter(customer -> MetinKarsilastirici.normalize(customer.getName()).equals(wanted))
                .findFirst();
    }

    /**
     * The existing customer with this name, or a new one with the given
     * contact details. An existing customer is returned unchanged.
     */
    @Transactional
    public CustomerMatch findOrCreate(String name, String phone, String address, String email) {
        Optional<Customer> existing = findByName(name);
        if (existing.isPresent()) {
            return new CustomerMatch(existing.get(), false);
        }
        Customer customer = new Customer(trimmed(name), trimmed(phone), trimmed(address), null, null);
        customer.setEmail(trimmed(email));
        return new CustomerMatch(create(customer), true);
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
        existing.setEmail(changes.getEmail());
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

    private static String trimmed(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    /** A customer found by name ({@code created == false}) or just added. */
    public record CustomerMatch(Customer customer, boolean created) {
    }
}
