package com.bankapp.repository;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Fetch-join user so account.getUser() never triggers LazyInitializationException
    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumber(@Param("accountNumber") String accountNumber);

    // Fetch-join user for findById as well
    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :id")
    Optional<Account> findByIdWithUser(@Param("id") Long id);

    List<Account> findByUser(User user);

    boolean existsByAccountNumber(String accountNumber);
}