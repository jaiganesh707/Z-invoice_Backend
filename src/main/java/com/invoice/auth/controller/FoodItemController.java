package com.invoice.auth.controller;

import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.FoodItemService;
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
import java.util.Optional;
import java.util.UUID;

import com.invoice.auth.dto.FoodItemDto;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/food-items")
@RequiredArgsConstructor
public class FoodItemController {
    private final FoodItemService foodItemService;
    private final AuthenticationService authenticationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FoodItemDto>> getAllFoodItems(
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal User requester) {

        // If a specific userId is requested, enforce access controls
        if (userId != null) {
            // Super Admin can view any user's items, others can only view THEIR OWN items
            if (requester.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN
                    && !requester.getId().equals(userId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: You can only view your own items");
            }

            Optional<User> userOpt = authenticationService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(List.of()); // Return empty list if user doesn't exist
            }

            User user = userOpt.get();
            List<FoodItem> items = foodItemService.getFoodItemsByUser(user);

            // STRICT ISOLATION: Return ONLY the user's items (empty list if none), NO
            // fallback to system items
            return ResponseEntity.ok(items.stream().map(this::mapToDto).collect(Collectors.toList()));
        }

        // If Super Admin and no userId, return ALL system items
        if (requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN) {
            return ResponseEntity.ok(foodItemService.allFoodItems().stream()
                    .map(this::mapToDto).collect(Collectors.toList()));
        }

        // Standard User context: return ONLY requester's items
        return ResponseEntity.ok(foodItemService.getFoodItemsByUser(requester).stream()
                .map(this::mapToDto).collect(Collectors.toList()));
    }

    private FoodItemDto mapToDto(FoodItem item) {
        return FoodItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .uniqueCode(item.getUniqueCode())
                .build();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FoodItem> createFoodItem(
            @RequestBody FoodItem foodItem,
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal User requester) {
        User targetUser = requester;

        if (userId != null && requester.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN) {
            targetUser = authenticationService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        }

        return ResponseEntity.ok(foodItemService.createFoodItem(foodItem, targetUser));
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FoodItem> uploadFile(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User requester) {
        FoodItem foodItem = foodItemService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));

        if (requester.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN &&
                !foodItem.getUser().getId().equals(requester.getId())) {
            throw new RuntimeException("Access denied: You can only update your own items");
        }

        try {
            String uploadDir = "uploads/";
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

            String fileUrl = "/uploads/" + newFilename;
            foodItem.setImageUrl(fileUrl);

            // Re-save via service's update (or repository directly, but we only need to
            // update the field)
            return ResponseEntity.ok(foodItemService.update(id, foodItem, requester));

        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FoodItem> updateFoodItem(
            @PathVariable Integer id,
            @RequestBody FoodItem foodItem,
            @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(foodItemService.update(id, foodItem, requester));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFoodItem(
            @PathVariable Integer id,
            @AuthenticationPrincipal User requester) {
        foodItemService.delete(id, requester);
        return ResponseEntity.noContent().build();
    }
}
