package com.invoice.auth.service;

import com.invoice.auth.dto.DeliveryOrderDto;
import com.invoice.auth.entity.DeliveryOrder;
import com.invoice.auth.entity.DeliveryStatus;
import com.invoice.auth.entity.Driver;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.DeliveryOrderRepository;
import com.invoice.auth.repository.DriverRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryOrderService {
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeliveryOrder createOrder(DeliveryOrderDto dto) {
        User customer = userRepository.findById(Objects.requireNonNull(dto.getCustomerId()))
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        DeliveryOrder order = DeliveryOrder.builder()
                .customer(customer)
                .shopName(dto.getShopName())
                .shopDetails(dto.getShopDetails())
                .pickupMessage(dto.getPickupMessage())
                .status(DeliveryStatus.PENDING)
                .build();

        if (dto.getDriverId() != null) {
            Driver driver = driverRepository.findByUserId(dto.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found"));
            order.setDriver(driver);
            order.setStatus(DeliveryStatus.ASSIGNED);
        }

        @SuppressWarnings("null")
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        return saved;
    }

    public List<DeliveryOrderDto> getOrdersForDriver(Integer driverUserId) {
        if (driverUserId == null) {
            throw new IllegalArgumentException("Driver User ID must not be null");
        }
        Driver driver = driverRepository.findByUserId(driverUserId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return deliveryOrderRepository.findByDriver(driver).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<DeliveryOrderDto> getOrdersForCustomer(Integer customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID must not be null");
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return deliveryOrderRepository.findByCustomer(customer).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryOrder updateOrderStatus(Integer orderId, DeliveryStatus status) {
        DeliveryOrder order = deliveryOrderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        @SuppressWarnings("null")
        DeliveryOrder saved = deliveryOrderRepository.save(order);
        return saved;
    }

    private DeliveryOrderDto mapToDto(DeliveryOrder order) {
        return DeliveryOrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .driverId(order.getDriver() != null ? order.getDriver().getId() : null)
                .shopName(order.getShopName())
                .shopDetails(order.getShopDetails())
                .pickupMessage(order.getPickupMessage())
                .status(order.getStatus())
                .build();
    }
}
