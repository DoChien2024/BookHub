package com.example.thuviensach01.service;

import com.example.thuviensach01.entity.Role;
import com.example.thuviensach01.entity.User;
import com.example.thuviensach01.model.Book;
import com.example.thuviensach01.model.Loan;
import com.example.thuviensach01.repository.BookRepository;
import com.example.thuviensach01.repository.LoanRepository;
import com.example.thuviensach01.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LoanService {

    private final BookRepository bookRepo;
    private final UserRepository userRepo;
    private final LoanRepository loanRepo;

    public LoanService(BookRepository bookRepo, UserRepository userRepo, LoanRepository loanRepo) {
        this.bookRepo = bookRepo;
        this.userRepo = userRepo;
        this.loanRepo = loanRepo;
    }

    @Transactional
    public Loan borrow(String username, Long bookId, int days) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Book book = bookRepo.findById(bookId).orElseThrow();

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Sách đã hết");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepo.save(book);

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setDueAt(Instant.now().plusSeconds(days * 86400L));

        return loanRepo.save(loan);
    }

    // Hàm mở rộng nhận thêm fullName, email, address
    @Transactional
    public Loan borrow(String username, Long bookId, int days, String fullName, String email, String address) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Book book = bookRepo.findById(bookId).orElseThrow();

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Sách đã hết");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepo.save(book);

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setDueAt(Instant.now().plusSeconds(days * 86400L));

        loan.setFullName(fullName);
        loan.setEmail(email);
        loan.setAddress(address);

        return loanRepo.save(loan);
    }

    @Transactional
    public Loan giveBack(String username, Long loanId) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (loan.getStatus() == Loan.Status.RETURNED) return loan;

        boolean isOwner = loan.getUser().getUsername().equals(username);
        boolean isAdmin = loan.getUser().getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Không có quyền trả phiếu mượn này");
        }

        loan.setStatus(Loan.Status.RETURNED);
        loan.setReturnedAt(Instant.now());
        loanRepo.save(loan);

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepo.save(book);

        return loan;
    }
}