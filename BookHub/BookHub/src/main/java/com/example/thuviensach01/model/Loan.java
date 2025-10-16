package com.example.thuviensach01.model;

import com.example.thuviensach01.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant borrowedAt = Instant.now();

    @Column(nullable = false)
    private Instant dueAt;

    private Instant returnedAt;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.IN_PROGRESS;

    // Thêm các trường lưu thông tin mượn
    @Column(length = 128)
    private String fullName;

    @Column(length = 128)
    private String email;

    @Column(length = 256)
    private String address;

    public enum Status { IN_PROGRESS, RETURNED }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(Instant borrowedAt) { this.borrowedAt = borrowedAt; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    // Getter/setter mới cho các trường thêm
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
}