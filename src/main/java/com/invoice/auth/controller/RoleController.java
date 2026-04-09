package com.invoice.auth.controller;

import com.invoice.auth.entity.RoleEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/roles")
@RestController
public class RoleController {

    // These roles are exclusively managed by Prime Users via Workflow Center.
    // Super Admin must NOT be able to assign them during user provisioning.
    private static final List<RoleEnum> PRIME_USER_ONLY_ROLES = Arrays.asList(
        RoleEnum.ROLE_APPROVER,
        RoleEnum.ROLE_WORKFLOW_USER
    );

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<RoleEnum>> allRoles() {
        List<RoleEnum> superAdminRoles = Arrays.stream(RoleEnum.values())
                .filter(role -> !PRIME_USER_ONLY_ROLES.contains(role))
                .collect(Collectors.toList());
        return ResponseEntity.ok(superAdminRoles);
    }
}
