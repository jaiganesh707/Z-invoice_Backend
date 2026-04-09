package com.invoice.auth.service;

import com.invoice.auth.entity.BusinessAsset;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.BusinessAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessAssetService {
    private final BusinessAssetRepository assetRepository;

    @Transactional
    public BusinessAsset createAsset(BusinessAsset asset) {
        Objects.requireNonNull(asset, "Asset must not be null");
        return assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public List<BusinessAsset> getAssetsByUser(User user) {
        Objects.requireNonNull(user, "User must not be null");
        
        // Hierarchical logic for fetching assets
        User primeIdentity = user.getParentUser() != null ? user.getParentUser() : user;
        
        return assetRepository.findAllByUserOrderByCreatedAtDesc(primeIdentity);
    }

    @Transactional
    public BusinessAsset updateAsset(Integer id, BusinessAsset details) {
        Objects.requireNonNull(id, "Asset ID must not be null");
        Objects.requireNonNull(details, "Asset details must not be null");
        Optional<BusinessAsset> optional = assetRepository.findById(id);
        if (optional.isPresent()) {
            BusinessAsset asset = optional.get();
            asset.setAssetName(details.getAssetName());
            asset.setDescription(details.getDescription());
            asset.setAssetImageUrl(details.getAssetImageUrl());
            asset.setTargetUrl(details.getTargetUrl());
            return assetRepository.save(asset);
        }
        return null;
    }

    @Transactional
    public void deleteAsset(Integer id) {
        Objects.requireNonNull(id, "Asset ID must not be null");
        assetRepository.deleteById(id);
    }
}
