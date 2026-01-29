package com.smartcity.governance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/citizen")
    public String citizen() {
        return "citizen";
    }

    @GetMapping("/officer")
    public String officer() {
        return "officer";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}

