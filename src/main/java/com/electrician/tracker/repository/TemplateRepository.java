package com.electrician.tracker.repository;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.Template;
import com.electrician.tracker.domain.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByTypeOrderByNameAsc(TemplateType type);

    List<Template> findAllByOrderByTypeAscNameAsc();

    Optional<Template> findFirstByTypeAndDefaultTemplateTrue(TemplateType type);

    boolean existsByTypeAndNameIgnoreCase(TemplateType type, String name);

    boolean existsByTypeAndNameIgnoreCaseAndIdNot(TemplateType type, String name, Long id);
}
