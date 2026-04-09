package com.invoice.auth.controller;

import com.invoice.auth.entity.BusinessAsset;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.BusinessAssetService;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssetController {
    private final BusinessAssetService assetService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<BusinessAsset> createAsset(@RequestBody BusinessAsset request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow();

        // Anchor asset to the Root Prime node identity
        User primeUser = currentUser.getParentUser() != null ? currentUser.getParentUser() : currentUser;

        request.setUser(primeUser);
        return ResponseEntity.ok(assetService.createAsset(request));
    }

    @GetMapping
    public ResponseEntity<List<BusinessAsset>> getAssets() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username).orElseThrow();

        return ResponseEntity.ok(assetService.getAssetsByUser(currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessAsset> updateAsset(@PathVariable Integer id, @RequestBody BusinessAsset request) {
        Objects.requireNonNull(id, "Asset ID must not be null");
        BusinessAsset updated = assetService.updateAsset(id, request);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Integer id) {
        Objects.requireNonNull(id, "Asset ID must not be null");
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
