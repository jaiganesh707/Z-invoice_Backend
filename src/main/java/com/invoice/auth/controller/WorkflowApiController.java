package com.invoice.auth.controller;

import com.invoice.auth.dto.WorkflowUserDto;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowApiController {
    private final WorkflowService workflowService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("#### WORKFLOW API V5: Operational at /workflow ####");
    }

    @GetMapping("/sub-users")
    @PreAuthorize("hasAnyRole('PRIME_USER', 'SUPER_ADMIN', 'APPROVER', 'USER', 'ADMIN')")
    public ResponseEntity<List<WorkflowUserDto>> getSubUsers(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer parentId) {
        Integer targetId = (parentId != null && (user.getRole().name().equals("ROLE_SUPER_ADMIN") || user.getRole().name().equals("ROLE_ADMIN"))) ? parentId : user.getId();
        log.info("#### WORKFLOW API: Fetching sub-units for target: {} (requested by {}) ####", targetId, user.getUsername());
        return ResponseEntity.ok(workflowService.getSubUsers(targetId));
    }

    @PostMapping("/sub-users")
    @PreAuthorize("hasAnyRole('PRIME_USER', 'SUPER_ADMIN')") // Only Prime Users or Super Admin can provision sub-units
    public ResponseEntity<WorkflowUserDto> createSubUser(
            @AuthenticationPrincipal User user,
            @RequestBody WorkflowUserDto dto,
            @RequestParam(required = false) Integer parentId) {
        Integer targetId = (parentId != null && (user.getRole().name().equals("ROLE_SUPER_ADMIN") || user.getRole().name().equals("ROLE_ADMIN"))) ? parentId : user.getId();
        log.info("#### WORKFLOW API: Provisioning sub-unit for target: {} ####", targetId);
        return ResponseEntity.ok(workflowService.createSubUser(targetId, dto));
    }

    @PutMapping("/sub-users/{id}")
    @PreAuthorize("hasAnyRole('PRIME_USER', 'SUPER_ADMIN')")
    public ResponseEntity<WorkflowUserDto> updateSubUser(@PathVariable Integer id, @RequestBody WorkflowUserDto dto) {
        return ResponseEntity.ok(workflowService.updateSubUser(id, dto));
    }

    @DeleteMapping("/sub-users/{id}")
    @PreAuthorize("hasAnyRole('PRIME_USER', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteSubUser(@AuthenticationPrincipal User user, @PathVariable Integer id) {
        workflowService.deleteSubUser(id, user.getId());
        return ResponseEntity.noContent().build();
    }

}
