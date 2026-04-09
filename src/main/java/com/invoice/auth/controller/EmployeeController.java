package com.invoice.auth.controller;

import com.invoice.auth.entity.Employee;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.EmployeeService;
import com.invoice.auth.service.AuthenticationService;
import com.invoice.auth.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    private final AuthenticationService authenticationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Employee>> getAll(
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal User requester) {
        
        User target = requester;
        
        // SuperAdmin can see anyone
        if (userId != null && (requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN || requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_ADMIN)) {
            target = authenticationService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        } else if (requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_HR || 
                   requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_MANAGEMENT ||
                   requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_SUPERVISOR) {
            // HR and Management see the Prime User's (parent's) employees
            if (requester.getParentUser() != null) {
                target = requester.getParentUser();
            }
        }
        
        return ResponseEntity.ok(employeeService.getEmployeesByUser(target));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> create(
            @RequestBody Employee employee,
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal User requester) {
        
        User target = requester;
        if (userId != null && (requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN || requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_ADMIN)) {
            target = authenticationService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        } else if (requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_HR || 
                   requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_MANAGEMENT) {
            if (requester.getParentUser() != null) {
                target = requester.getParentUser();
            }
        }
        
        return ResponseEntity.ok(employeeService.createEmployee(employee, target));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> update(
            @PathVariable Integer id,
            @RequestBody Employee employee,
            @AuthenticationPrincipal User requester) {
        
        User targetOwner = requester;
        if ((requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_HR || 
             requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_MANAGEMENT) && 
            requester.getParentUser() != null) {
            targetOwner = requester.getParentUser();
        }

        return ResponseEntity.ok(employeeService.updateEmployee(id, employee, targetOwner));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal User requester) {
        
        User targetOwner = requester;
        if ((requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_HR || 
             requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_MANAGEMENT) && 
            requester.getParentUser() != null) {
            targetOwner = requester.getParentUser();
        }

        employeeService.deleteEmployee(id, targetOwner);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> uploadImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User requester) {
        
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (!employee.getUser().getId().equals(requester.getId())) {
             throw new RuntimeException("Access denied");
        }

        try {
            String uploadDir = "uploads/employees/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/employees/" + newFilename;
            employee.setImageUrl(fileUrl);

            return ResponseEntity.ok(employeeService.updateEmployee(id, employee, requester));

        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
}
