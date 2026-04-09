package com.invoice.auth.dto;

import com.invoice.auth.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderDto {
    private Integer id;
    private Integer customerId;
    private Integer driverId;
    private String shopName;
    private String shopDetails;
    private String pickupMessage;
    private DeliveryStatus status;
}
