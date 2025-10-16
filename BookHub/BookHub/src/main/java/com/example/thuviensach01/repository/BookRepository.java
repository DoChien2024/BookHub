package com.example.thuviensach01.repository;

import com.example.thuviensach01.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> { }