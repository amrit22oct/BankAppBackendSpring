package com.bankapp.service;

import com.bankapp.dto.AccountRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public Account createAccount(AccountRequest request, String email) {
        User user = userService.getCurrentUserEntity(email);

        if (request.getInitialDeposit() != null
                && request.getInitialDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Initial deposit cannot be negative");
        }

        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.generate())
                .accountType(request.getAccountType())
                .balance(request.getInitialDeposit() != null
                        ? request.getInitialDeposit()
                        : BigDecimal.ZERO)
                .user(user)
                .build();
        // status defaults to ACTIVE via @Builder.Default

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUser(String email) {
        User user = userService.getCurrentUserEntity(email);
        return accountRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        // Uses FETCH JOIN — account.getUser() is always safe, no LazyInitializationException
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        // Uses FETCH JOIN — user is eagerly loaded
        return accountRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
    }

    /**
     * Used by TransactionService to persist balance changes.
     * Keeps AccountRepository private to AccountService only.
     */
    @Transactional
    public Account save(Account account) {
        return accountRepository.save(account);
    }

    @Transactional
    public Account closeAccount(Long accountId, String email) {
        User user = userService.getCurrentUserEntity(email);
        Account account = getAccountById(accountId);

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to close this account");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot close account with non-zero balance");
        }

        account.setStatus(Account.AccountStatus.CLOSED);
        return accountRepository.save(account);
    }
}