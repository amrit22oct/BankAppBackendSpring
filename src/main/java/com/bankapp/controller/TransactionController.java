package com.bankapp.controller;

import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Transaction;
import com.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    // Deposit money into own account
    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @RequestParam String accountNumber,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                transactionService.deposit(accountNumber, amount, userDetails.getUsername()));
    }

    // Withdraw money from own account
    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(
            @RequestParam String accountNumber,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                transactionService.withdraw(accountNumber, amount, userDetails.getUsername()));
    }

    // Transfer between two accounts
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                transactionService.transfer(request, userDetails.getUsername()));
    }

    // Get full transaction history for an account
    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<Transaction>> getHistory(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                transactionService.getTransactionHistory(accountNumber, userDetails.getUsername()));
    }

    // Get a single transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}