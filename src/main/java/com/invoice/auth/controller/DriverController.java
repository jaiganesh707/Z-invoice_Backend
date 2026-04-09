package com.invoice.auth.controller;

import com.invoice.auth.dto.DriverDetailsDto;
import com.invoice.auth.dto.DriverRegisterDto;
import com.invoice.auth.entity.Driver;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService driverService;

    @PostMapping("/details")
    public ResponseEntity<Driver> updateDetails(@RequestBody DriverDetailsDto dto) {
        return ResponseEntity.ok(driverService.updateDriverDetails(dto));
    }

    @PostMapping("/register-driver")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Driver> registerDriver(@RequestBody DriverRegisterDto dto) {
        return ResponseEntity.ok(driverService.registerDriver(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<DriverDetailsDto> getMyDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(driverService.getDriverDetails(currentUser.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<List<DriverDetailsDto>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/by-parent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'PRIME_USER', 'APPROVER', 'WORKFLOW_USER')")
    public ResponseEntity<List<DriverDetailsDto>> getDriversByParent(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(driverService.getDriversByParent(user));
    }

    @PostMapping("/{userId}/license")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Driver> uploadLicense(
            @PathVariable Integer userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String uploadDir = "uploads/license/";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = java.util.UUID.randomUUID().toString() + extension;
            java.nio.file.Path filePath = uploadPath.resolve(newFilename);
            java.nio.file.Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/license/" + newFilename;
            return ResponseEntity.ok(driverService.updateLicenseUrl(userId, fileUrl));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/photo")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Driver> uploadPhoto(
            @PathVariable Integer userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String uploadDir = "uploads/drivers/";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = java.util.UUID.randomUUID().toString() + extension;
            java.nio.file.Path filePath = uploadPath.resolve(newFilename);
            java.nio.file.Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/drivers/" + newFilename;
            return ResponseEntity.ok(driverService.updateDriverPhotoUrl(userId, fileUrl));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
}
