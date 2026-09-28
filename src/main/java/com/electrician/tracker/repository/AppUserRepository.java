package com.electrician.tracker.repository;

import java.util.List;
import java.util.Optional;

import com.electrician.tracker.domain.AppUser;
import com.electrician.tracker.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRoleAndActiveTrue(UserRole role);

    List<AppUser> findAllByOrderByFullNameAsc();
}
