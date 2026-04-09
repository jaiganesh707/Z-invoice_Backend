package com.invoice.auth.controller;

import com.invoice.auth.dto.RegisterUserDto;
import com.invoice.auth.dto.UpdateProfileDto;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final AuthenticationService authenticationService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> authenticatedUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = authenticationService.allUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or #id.equals(#requester.id) or (#requester.parentUser != null and #id.equals(#requester.parentUser.id))")
    public ResponseEntity<User> getUserById(@PathVariable Integer id, @AuthenticationPrincipal User requester) {
        log.info("Fetching user with id: {}", id);
        return authenticationService.findById(id)
                .map(user -> {
                    log.info("Found user: {}", user.getUsername());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    log.warn("User not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@Valid @RequestBody RegisterUserDto registerUserDto) {
        if (registerUserDto.getRole() == null) {
            registerUserDto.setRole(RoleEnum.ROLE_USER);
        }
        User createdUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateProfile(
            @Valid @RequestBody UpdateProfileDto updateDto,
            @AuthenticationPrincipal User currentUser) {

        User updatedUser = authenticationService.updateUser(currentUser.getId(), updateDto.getEmail(),
                updateDto.getContactNumber());
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody RegisterUserDto updateDto) {
        return ResponseEntity.ok(authenticationService.updateFullUser(id, updateDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        authenticationService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<User> uploadImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        try {
            String uploadDir = "uploads/users/";
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

            String fileUrl = "/uploads/users/" + newFilename;
            log.info("Asset digitized for user {}: {}", id, fileUrl);
            return ResponseEntity.ok(authenticationService.updateImageUrl(id, fileUrl));
        } catch (IOException e) {
            log.error("Failed to digitize asset for user {}", id, e);
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
}
