package com.invoice.auth.controller;

import com.invoice.auth.dto.CustomerDto;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER', 'WORKFLOW_USER', 'APPROVER', 'MANAGEMENT')")
    public ResponseEntity<List<CustomerDto>> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer userId) {
        // Workflow users pass their Prime User's ID as userId param
        int effectiveUserId = (userId != null) ? userId : user.getId();
        return ResponseEntity.ok(customerService.getAllCustomersByUser(effectiveUserId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER')")
    public ResponseEntity<CustomerDto> create(@AuthenticationPrincipal User user, @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(customerService.createCustomer(user.getId(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER')")
    public ResponseEntity<CustomerDto> update(@PathVariable Integer id, @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay-pending/{date}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PRIME_USER', 'APPROVER')")
    public ResponseEntity<Void> payPendingAmount(@PathVariable Integer id, @PathVariable String date) {
        customerService.payCustomerPendingAmount(id, java.time.LocalDate.parse(date));
        return ResponseEntity.noContent().build();
    }
}
