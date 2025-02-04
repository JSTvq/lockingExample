package com.kir138.repository;

import com.kir138.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select a FROM Account a WHERE a.id =:id")
    Optional<Account> findByIdWithPessimisticLock(Long id);

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Account> findById(Long id);
}
