package com.accommodation.platform.user.repository;

import com.accommodation.platform.common.enums.AccountStatus;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<User> findByRole(UserRole role, Pageable pageable);
    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);
    Page<User> findByRoleAndAccountStatus(UserRole role, AccountStatus status, Pageable pageable);
    long countByRole(UserRole role);
}
