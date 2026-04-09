package com.invoice.auth.controller;

import com.invoice.auth.dto.DeliveryOrderDto;
import com.invoice.auth.entity.DeliveryOrder;
import com.invoice.auth.entity.DeliveryStatus;
import com.invoice.auth.entity.User;
import com.invoice.auth.service.DeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/delivery-orders")
@RequiredArgsConstructor
public class DeliveryOrderController {
    private final DeliveryOrderService deliveryOrderService;

    @PostMapping("/create")
    public ResponseEntity<DeliveryOrder> createOrder(@RequestBody DeliveryOrderDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        dto.setCustomerId(currentUser.getId());
        return ResponseEntity.ok(deliveryOrderService.createOrder(dto));
    }

    @GetMapping("/driver")
    public ResponseEntity<List<DeliveryOrderDto>> getDriverOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(deliveryOrderService.getOrdersForDriver(currentUser.getId()));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<DeliveryOrderDto>> getCustomerOrders() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(deliveryOrderService.getOrdersForCustomer(currentUser.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryOrder> updateStatus(@PathVariable Integer id, @RequestParam DeliveryStatus status) {
        return ResponseEntity.ok(deliveryOrderService.updateOrderStatus(id, status));
    }
}
