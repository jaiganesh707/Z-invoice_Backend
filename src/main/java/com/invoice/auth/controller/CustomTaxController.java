package com.invoice.auth.controller;

import com.invoice.auth.dto.CustomTaxDto;
import com.invoice.auth.service.CustomTaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/taxes")
@RestController
@RequiredArgsConstructor
public class CustomTaxController {
    private final CustomTaxService customTaxService;

    @PostMapping
    @PreAuthorize("isAuthenticated() and (hasRole('SUPER_ADMIN') or hasRole('PRIME_USER'))")
    public ResponseEntity<CustomTaxDto> createForUser(@RequestParam Integer userId, @RequestBody CustomTaxDto dto) {
        return ResponseEntity.ok(customTaxService.createTax(dto, userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and (hasRole('SUPER_ADMIN') or hasRole('PRIME_USER'))")
    public ResponseEntity<List<CustomTaxDto>> getAllForUser(@RequestParam Integer userId) {
        return ResponseEntity.ok(customTaxService.getAllByUser(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (hasRole('SUPER_ADMIN') or hasRole('PRIME_USER'))")
    public ResponseEntity<CustomTaxDto> updateForUser(@PathVariable Integer id, @RequestParam Integer userId, @RequestBody CustomTaxDto dto) {
        return ResponseEntity.ok(customTaxService.updateTax(id, dto, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (hasRole('SUPER_ADMIN') or hasRole('PRIME_USER'))")
    public ResponseEntity<Void> deleteForUser(@PathVariable Integer id, @RequestParam Integer userId) {
        customTaxService.deleteTax(id, userId);
        return ResponseEntity.ok().build();
    }

    // Active taxes for the specific user/stakeholder to use during billing
    @GetMapping("/active/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomTaxDto>> getActiveTaxes(@PathVariable Integer userId) {
        return ResponseEntity.ok(customTaxService.getActiveTaxesByUser(userId));
    }
}
