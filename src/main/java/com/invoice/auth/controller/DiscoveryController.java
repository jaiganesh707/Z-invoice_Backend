package com.invoice.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
public class DiscoveryController {
    
    @GetMapping("/discovery-check")
    public String check() {
        return "System Discovery Protocol: ONLINE - Verified at " + LocalDateTime.now();
    }
}
