package com.eventledger.account.repository;

import com.eventledger.account.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Find transaction by event ID (for idempotency)
     */
    Optional<Transaction> findByEventId(String eventId);

    /**
     * Get all transactions for an account, ordered by event timestamp
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId ORDER BY t.eventTimestamp ASC")
    List<Transaction> findTransactionsByAccountId(@Param("accountId") String accountId);

    /**
     * Get recent transactions for an account (up to limit, ordered by event timestamp descending)
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId ORDER BY t.eventTimestamp DESC LIMIT :limit")
    List<Transaction> findRecentTransactionsByAccountId(@Param("accountId") String accountId, @Param("limit") int limit);

    /**
     * Check if transaction with event ID exists
     */
    boolean existsByEventId(String eventId);

    /**
     * Get all transactions for an account
     */
    List<Transaction> findAllByAccountId(String accountId);
}