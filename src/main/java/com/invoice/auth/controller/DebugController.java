package com.invoice.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class DebugController {
    @GetMapping("/api-alive-check")
    public ResponseEntity<String> alive() {
        return ResponseEntity.ok("Backend is definitely running and responding to root paths.");
    }
}
