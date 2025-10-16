package com.example.thuviensach01.controller;

import com.example.thuviensach01.dto.BorrowRequest;
import com.example.thuviensach01.model.Loan;
import com.example.thuviensach01.repository.LoanRepository;
import com.example.thuviensach01.service.LoanService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanRepository loanRepo;

    public LoanController(LoanService loanService, LoanRepository loanRepo) {
        this.loanService = loanService;
        this.loanRepo = loanRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid BorrowRequest req, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        try {
            Loan loan = loanService.borrow(
                    auth.getName(),
                    req.getBookId(),
                    req.getDays(),
                    req.getFullName(),
                    req.getEmail(),
                    req.getAddress());

            return ResponseEntity.ok(Map.of(
                    "id", loan.getId(),
                    "bookId", loan.getBook().getId(),
                    "dueAt", loan.getDueAt()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<?> giveBack(@PathVariable Long id, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        Loan loan = loanService.giveBack(auth.getName(), id);
        return ResponseEntity.ok(Map.of("status", loan.getStatus().name()));
    }


    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean mine,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Authentication auth) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "borrowedAt"));
        if (Boolean.TRUE.equals(mine)) {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
            }
            return ResponseEntity.ok(loanRepo.findByUserUsernameOrderByBorrowedAtDesc(auth.getName(), p));
        }
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ ADMIN xem tất cả"));
        }
        return ResponseEntity.ok(loanRepo.findAll(p));
    }
}