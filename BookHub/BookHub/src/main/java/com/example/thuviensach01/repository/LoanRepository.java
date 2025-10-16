package com.example.thuviensach01.repository;

import com.example.thuviensach01.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Page<Loan> findByUserUsernameOrderByBorrowedAtDesc(String username, Pageable pageable);
}