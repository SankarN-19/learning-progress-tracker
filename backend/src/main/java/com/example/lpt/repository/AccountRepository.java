package com.example.lpt.repository;

import com.example.lpt.entity.Account;
import com.example.lpt.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
