package com.bankapp.controller;

import com.bankapp.dto.AccountRequest;
import com.bankapp.model.Account;
import com.bankapp.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // Create a new account for the logged-in user
    @PostMapping
    public ResponseEntity<Account> createAccount(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.createAccount(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    // Get all accounts for the logged-in user
    @GetMapping
    public ResponseEntity<List<Account>> getMyAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(accountService.getAccountsByUser(userDetails.getUsername()));
    }

    // Get a specific account by account number (must belong to logged-in user)
    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.getAccountByNumber(accountNumber);
        // Ownership check – users can only see their own accounts
        if (!account.getUser().getEmail().equals(userDetails.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(account);
    }

    // Close an account (balance must be zero)
    @PatchMapping("/{id}/close")
    public ResponseEntity<Account> closeAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Account account = accountService.closeAccount(id, userDetails.getUsername());
        return ResponseEntity.ok(account);
    }
}