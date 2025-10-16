package com.example.thuviensach01.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoansViewController {
    @GetMapping("/loans")
    public String myLoans() {
        return "loans";
    }
}