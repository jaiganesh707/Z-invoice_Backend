package com.invoice.auth.service;

import com.invoice.auth.exception.ResourceNotFoundException;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.InvoiceItem;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.FoodItemRepository;
import com.invoice.auth.repository.InvoiceRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional
    @SuppressWarnings("null")
    public Invoice createInvoice(CreateInvoiceDto dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Invoice must contain at least one item");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Invoice invoice = new Invoice();
        invoice.setUser(user);

        for (CreateInvoiceDto.InvoiceItemDto itemDto : dto.getItems()) {
            if (itemDto.getFoodItemId() == null || itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid item data: quantity must be positive and food item ID must not be null");
            }
            FoodItem foodItem = foodItemRepository.findById(itemDto.getFoodItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Food item not found with id: " + itemDto.getFoodItemId()));

            BigDecimal itemTotal = foodItem.getPrice().multiply(new BigDecimal(itemDto.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .foodItem(foodItem)
                    .quantity(itemDto.getQuantity())
                    .price(foodItem.getPrice())
                    .build();

            invoiceItems.add(invoiceItem);
        }

        invoice.setItems(invoiceItems);
        invoice.setTotalAmount(totalAmount);

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        // Force initialization for JSON serialization outside transaction
        invoices.forEach(i -> {
            if (i.getItems() != null)
                i.getItems().size();
        });
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByUser(Integer userId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        User user = userRepository.findById((int) userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Invoice> invoices;
        if (start != null && end != null) {
            invoices = invoiceRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, start, end);
        } else {
            invoices = invoiceRepository.findAllByUserOrderByCreatedAtDesc(user);
        }

        // Force initialization for JSON serialization outside transaction
        invoices.forEach(i -> {
            if (i.getItems() != null) {
                i.getItems().size();
                i.getItems().forEach(item -> {
                    if (item.getFoodItem() != null) {
                        item.getFoodItem().getName(); // Touch the food item too
                    }
                });
            }
        });

        return invoices;
    }
}
