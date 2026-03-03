package com.invoice.auth.controller;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Invoice>> getInvoicesByUser(
            @PathVariable Integer userId,
            @AuthenticationPrincipal User requester) {

        if (requester.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN
                && !requester.getId().equals(userId)) {
            throw new RuntimeException("Access denied: You can only view your own invoices");
        }

        return ResponseEntity.ok(invoiceService.getInvoicesByUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }
}
