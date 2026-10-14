package com.electrician.tracker.repository.settings;

import com.electrician.tracker.domain.settings.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
