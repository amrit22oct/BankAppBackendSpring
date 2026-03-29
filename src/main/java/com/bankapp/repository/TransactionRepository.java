package com.bankapp.repository;

import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount = :account OR t.destinationAccount = :account ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccount(@Param("account") Account account);

    List<Transaction> findBySourceAccount(Account account);

    List<Transaction> findByDestinationAccount(Account account);
}