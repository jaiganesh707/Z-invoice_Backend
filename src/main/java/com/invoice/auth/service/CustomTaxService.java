package com.invoice.auth.service;

import com.invoice.auth.dto.CustomTaxDto;
import com.invoice.auth.entity.CustomTax;
import com.invoice.auth.entity.User;
import com.invoice.auth.exception.ResourceNotFoundException;
import com.invoice.auth.repository.CustomTaxRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomTaxService {
    private final CustomTaxRepository customTaxRepository;
    private final UserRepository userRepository;

    @Transactional
    @SuppressWarnings("null")
    public CustomTaxDto createTax(CustomTaxDto dto, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomTax tax = CustomTax.builder()
                .user(user)
                .name(dto.getName())
                .percentage(dto.getPercentage())
                .description(dto.getDescription())
                .isActive(dto.isActive())
                .build();

        CustomTax savedTax = customTaxRepository.save(tax);
        return mapToDto(savedTax);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public List<CustomTaxDto> getAllByUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return customTaxRepository.findAllByUser(user)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public List<CustomTaxDto> getActiveTaxesByUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return customTaxRepository.findAllByUserAndIsActiveTrue(user)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("null")
    public CustomTaxDto updateTax(Integer taxId, CustomTaxDto dto, Integer userId) {
        CustomTax tax = customTaxRepository.findById(taxId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));

        if (!tax.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only edit your own taxes");
        }

        tax.setName(dto.getName());
        tax.setPercentage(dto.getPercentage());
        tax.setDescription(dto.getDescription());
        tax.setActive(dto.isActive());

        CustomTax updatedTax = customTaxRepository.save(tax);
        return mapToDto(updatedTax);
    }

    @Transactional
    @SuppressWarnings("null")
    public void deleteTax(Integer taxId, Integer userId) {
        CustomTax tax = customTaxRepository.findById(taxId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));

        if (!tax.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only delete your own taxes");
        }

        customTaxRepository.delete(tax);
    }

    private CustomTaxDto mapToDto(CustomTax tax) {
        return CustomTaxDto.builder()
                .id(tax.getId())
                .userId(tax.getUser().getId())
                .name(tax.getName())
                .percentage(tax.getPercentage())
                .description(tax.getDescription())
                .isActive(tax.isActive())
                .build();
    }
}
