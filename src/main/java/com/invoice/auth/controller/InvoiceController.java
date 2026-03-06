package com.invoice.auth.controller;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('USER')")
    public ResponseEntity<Invoice> createInvoice(@Valid @RequestBody CreateInvoiceDto dto) {
        return ResponseEntity.ok(invoiceService.createInvoice(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or (hasRole('USER') and #userId.equals(principal.id))")
    public ResponseEntity<List<Invoice>> getUserInvoices(
            @PathVariable Integer userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        return ResponseEntity.ok(invoiceService.getInvoicesByUser(userId, startDate, endDate));
    }
}
