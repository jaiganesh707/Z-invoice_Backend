package com.invoice.auth.service;

import com.invoice.auth.dto.CustomerDto;
import com.invoice.auth.entity.Customer;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.CustomerRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public List<CustomerDto> getAllCustomersByUser(Integer userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return customerRepository.findAllByUserAndIsActiveTrue(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerDto createCustomer(Integer userId, CustomerDto dto) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Customer customer = Customer.builder()
                .companyName(dto.getCompanyName())
                .customerName(dto.getCustomerName())
                .contactNumber(dto.getContactNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .pinCode(dto.getPinCode())
                .gstin(dto.getGstin())
                .email(dto.getEmail())
                .customerType(dto.getCustomerType())
                .user(user)
                .build();
        
        @SuppressWarnings("null")
        Customer savedCustomer = customerRepository.save(customer);
        return mapToDto(savedCustomer);
    }

    @Transactional
    public CustomerDto updateCustomer(Integer customerId, CustomerDto dto) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId))
                .filter(Customer::getIsActive)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        customer.setCompanyName(dto.getCompanyName());
        customer.setCustomerName(dto.getCustomerName());
        customer.setContactNumber(dto.getContactNumber());
        customer.setAddress(dto.getAddress());
        customer.setCity(dto.getCity());
        customer.setState(dto.getState());
        customer.setPinCode(dto.getPinCode());
        customer.setGstin(dto.getGstin());
        customer.setEmail(dto.getEmail());
        customer.setCustomerType(dto.getCustomerType());
        
        return mapToDto(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Integer customerId) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setIsActive(false);
        customerRepository.save(customer);
    }

    @Transactional
    public void payCustomerPendingAmount(Integer customerId, java.time.LocalDate dateKey) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        if (customer.getPendingHistory() != null && customer.getPendingHistory().containsKey(dateKey)) {
            java.math.BigDecimal amountToClear = customer.getPendingHistory().get(dateKey);
            customer.getPendingHistory().remove(dateKey);
            
            java.math.BigDecimal currentOutstanding = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal newOutstanding = currentOutstanding.subtract(amountToClear);
            if (newOutstanding.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newOutstanding = java.math.BigDecimal.ZERO;
            }
            customer.setOutstandingBalance(newOutstanding);
            customerRepository.save(customer);
        }
    }

    private CustomerDto mapToDto(Customer customer) {
        return CustomerDto.builder()
                .id(customer.getId())
                .companyName(customer.getCompanyName())
                .customerName(customer.getCustomerName())
                .contactNumber(customer.getContactNumber())
                .address(customer.getAddress())
                .city(customer.getCity())
                .state(customer.getState())
                .pinCode(customer.getPinCode())
                .gstin(customer.getGstin())
                .email(customer.getEmail())
                .customerType(customer.getCustomerType())
                .outstandingBalance(customer.getOutstandingBalance())
                .pendingHistory(customer.getPendingHistory())
                .build();
    }
}
