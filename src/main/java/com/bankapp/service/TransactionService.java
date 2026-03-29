package com.bankapp.service;

import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    // No AccountRepository here — all account access goes through AccountService
    // which uses FETCH JOIN queries, so account.getUser() is always safe

    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount, String email) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be greater than zero");
        }

        Account account = accountService.getAccountByNumber(accountNumber);
        validateAccountOwnership(account, email);
        validateAccountActive(account);

        account.setBalance(account.getBalance().add(amount));
        accountService.save(account);

        return transactionRepository.save(
                Transaction.builder()
                        .referenceNumber(generateReference())
                        .destinationAccount(account)
                        .amount(amount)
                        .type(Transaction.TransactionType.DEPOSIT)
                        .status(Transaction.TransactionStatus.SUCCESS)
                        .description("Deposit to account " + accountNumber)
                        .build()
        );
    }

    @Transactional
    public Transaction withdraw(String accountNumber, BigDecimal amount, String email) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be greater than zero");
        }

        Account account = accountService.getAccountByNumber(accountNumber);
        validateAccountOwnership(account, email);
        validateAccountActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountService.save(account);

        return transactionRepository.save(
                Transaction.builder()
                        .referenceNumber(generateReference())
                        .sourceAccount(account)
                        .amount(amount)
                        .type(Transaction.TransactionType.WITHDRAWAL)
                        .status(Transaction.TransactionStatus.SUCCESS)
                        .description("Withdrawal from account " + accountNumber)
                        .build()
        );
    }

    @Transactional
    public Transaction transfer(TransferRequest request, String email) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new RuntimeException("Source and destination accounts cannot be the same");
        }

        Account source      = accountService.getAccountByNumber(request.getSourceAccountNumber());
        Account destination = accountService.getAccountByNumber(request.getDestinationAccountNumber());

        validateAccountOwnership(source, email);
        validateAccountActive(source);
        validateAccountActive(destination);

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        accountService.save(source);
        accountService.save(destination);

        return transactionRepository.save(
                Transaction.builder()
                        .referenceNumber(generateReference())
                        .sourceAccount(source)
                        .destinationAccount(destination)
                        .amount(request.getAmount())
                        .type(Transaction.TransactionType.TRANSFER)
                        .status(Transaction.TransactionStatus.SUCCESS)
                        .description(request.getDescription() != null
                                ? request.getDescription()
                                : "Transfer from " + source.getAccountNumber()
                                  + " to " + destination.getAccountNumber())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountNumber, String email) {
        Account account = accountService.getAccountByNumber(accountNumber);
        validateAccountOwnership(account, email);
        return transactionRepository.findAllByAccount(account);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void validateAccountOwnership(Account account, String email) {
        // account.getUser() is safe — FETCH JOIN in AccountService loads user eagerly
        if (!account.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: account does not belong to current user");
        }
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account " + account.getAccountNumber() + " is not active");
        }
    }

    private String generateReference() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}