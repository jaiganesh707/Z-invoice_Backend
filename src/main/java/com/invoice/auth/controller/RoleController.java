package com.invoice.auth.controller;

import com.invoice.auth.entity.RoleEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RequestMapping("/roles")
@RestController
public class RoleController {

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RoleEnum>> allRoles() {
        return ResponseEntity.ok(Arrays.asList(RoleEnum.values()));
    }
}
