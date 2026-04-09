package com.invoice.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CreateInvoiceDto {
    @NotNull(message = "User ID must not be null")
    private Integer userId;

    private Integer creatorId;
    
    private Integer customerId;
    
    private String status;
    
    private java.math.BigDecimal paidAmount;
    private java.math.BigDecimal outstandingAmount;
    private boolean deliveryRequired;
    private String billingAddress;
    private String customerGstin;

    @NotEmpty(message = "Invoice must contain at least one item")
    private List<InvoiceItemDto> items;

    @Data
    public static class InvoiceItemDto {
        private Integer foodItemId;
        private Integer quantity;
    }
}
