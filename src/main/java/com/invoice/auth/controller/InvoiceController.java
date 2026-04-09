package com.invoice.auth.controller;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping("/action-set-pending/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Invoice> markAsPending(@PathVariable("id") Integer id) {
        log.info("#### [TERMINAL SIGNAL] EXECUTING PENDING TRANSITION FOR INVOICE {} ####", id);
        return ResponseEntity.ok(invoiceService.markAsPending(id));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Invoice Controller is Active");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'USER', 'PRIME_USER', 'WORKFLOW_USER')")
    public ResponseEntity<Invoice> createInvoice(@Valid @RequestBody CreateInvoiceDto dto) {
        return ResponseEntity.ok(invoiceService.createInvoice(dto));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER')")
    public ResponseEntity<List<Invoice>> getPendingInvoices(
            @AuthenticationPrincipal com.invoice.auth.entity.User user) {
        if (user == null || user.getId() == null)
            return ResponseEntity.status(401).build();
        log.info("#### FETCHING QUEUE FOR USER: {} (ROLE: {}) ####", user.getUsername(), user.getRole());
        log.info("#### DEBUG: User ID: {}, Role: {} ####", user.getId(), user.getRole());
        return ResponseEntity.ok(invoiceService.getPendingInvoices(user));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER')")
    public ResponseEntity<Invoice> approveInvoice(@PathVariable Integer id,
            @AuthenticationPrincipal com.invoice.auth.entity.User user,
            @RequestParam(defaultValue = "false") boolean addOutstanding,
            @RequestParam(required = false) java.math.BigDecimal paidAmount) {
        return ResponseEntity.ok(invoiceService.approveInvoice(id, user.getId(), addOutstanding, paidAmount));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER', 'ADMIN')")
    public ResponseEntity<Invoice> updatePayment(@PathVariable Integer id,
            @RequestParam java.math.BigDecimal amount) {
        return ResponseEntity.ok(invoiceService.updatePayment(id, amount));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER')")
    public ResponseEntity<Invoice> rejectInvoice(@PathVariable Integer id, @RequestBody String reason) {
        return ResponseEntity.ok(invoiceService.rejectInvoice(id, reason));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('PRIME_USER', 'USER', 'WORKFLOW_USER')")
    public ResponseEntity<Invoice> submitForApproval(
            @PathVariable Integer id,
            @RequestBody(required = false) String submissionNote) {
        log.info("#### [SUBMIT-FOR-APPROVAL] Invoice {} submitted with note: {} ####", id, submissionNote);
        return ResponseEntity.ok(invoiceService.submitForApproval(id, submissionNote));
    }



    @PostMapping("/{id}/assign-driver")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER', 'WORKFLOW_USER')")
    public ResponseEntity<Invoice> assignDriver(@PathVariable Integer id, @RequestBody Integer driverUserId) {
        return ResponseEntity.ok(invoiceService.assignDriverToInvoice(id, driverUserId));
    }

    @PostMapping("/{id}/delivery-status")
    @PreAuthorize("hasAnyRole('DRIVER', 'APPROVER', 'PRIME_USER')")
    public ResponseEntity<Invoice> updateDeliveryStatus(@PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) java.math.BigDecimal amountCollected) {
        return ResponseEntity.ok(invoiceService.updateDeliveryStatus(id, status, amountCollected));
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAnyRole('APPROVER', 'PRIME_USER')")
    public ResponseEntity<Invoice> settleDelivery(@PathVariable Integer id) {
        return ResponseEntity.ok(invoiceService.settleDelivery(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or (hasAnyRole('USER', 'PRIME_USER', 'WORKFLOW_USER', 'APPROVER', 'DRIVER') and (#userId.equals(#requester.id) or (#requester.parentUser != null and #userId.equals(#requester.parentUser.id))))")
    public ResponseEntity<List<Invoice>> getUserInvoices(
            @PathVariable("userId") Integer userId,
            @AuthenticationPrincipal com.invoice.auth.entity.User requester,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {
        return ResponseEntity.ok(invoiceService.getInvoicesByUser(userId, startDate, endDate));
    }

    @GetMapping("/customer/{customerId}/balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER', 'APPROVER')")
    public ResponseEntity<BigDecimal> getCustomerBalance(@PathVariable Integer customerId) {
        return ResponseEntity.ok(invoiceService.getCustomerBalance(customerId));
    }

    @DeleteMapping("/terminate/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER', 'ADMIN', 'USER')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable("id") Integer id) {
        log.info("#### TERMINATION REQUEST RECEIVED FOR INVOICE ID: {} ####", id);
        invoiceService.deleteInvoice(id);
        log.info("#### INVOICE {} TERMINATED SUCCESSFULLY ####", id);
        return ResponseEntity.noContent().build();
    }
}
