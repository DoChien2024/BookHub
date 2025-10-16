package com.example.thuviensach01.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping({"/", "/index"})
    public String index() { return "index"; }       // -> templates/index.html

    @GetMapping("/login")
    public String login() { return "login"; }       // -> templates/login.html

    @GetMapping("/register")
    public String register() { return "register"; } // -> templates/register.html
}