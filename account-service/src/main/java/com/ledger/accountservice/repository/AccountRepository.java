package com.eventledger.account.repository;

import com.eventledger.account.entity.Account;
import com.eventledger.account.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Find account by ID
     */
    Optional<Account> findById(String accountId);

    /**
     * Delete account by ID
     */
    void deleteById(String accountId);
}