package com.invoice.auth.service;

import com.invoice.auth.dto.CustomerDto;
import com.invoice.auth.entity.Customer;
import com.invoice.auth.entity.User;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.repository.CustomerRepository;
import com.invoice.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    private User testUser;
    private Customer activeCustomer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("primeuser");
        testUser.setRole(RoleEnum.ROLE_PRIME_USER);

        activeCustomer = Customer.builder()
                .id(10)
                .companyName("Acme Corp")
                .customerName("John Doe")
                .email("john@acme.com")
                .contactNumber("9876543210")
                .address("123 Market St")
                .city("Mumbai")
                .state("Maharashtra")
                .pinCode("400001")
                .customerType("RETAIL")
                .gstin("GST123456")
                .isActive(true)
                .outstandingBalance(BigDecimal.ZERO)
                .pendingHistory(new HashMap<>())
                .user(testUser)
                .build();
    }

    // ─────────────────────── GET ALL CUSTOMERS ───────────────────────

    @Nested
    @DisplayName("getAllCustomersByUser")
    class GetAllCustomers {

        @Test
        @DisplayName("should return active customers for a valid user")
        void testGetAll_Success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(customerRepository.findAllByUserAndIsActiveTrue(testUser))
                    .thenReturn(List.of(activeCustomer));

            List<CustomerDto> result = customerService.getAllCustomersByUser(1);

            assertEquals(1, result.size());
            assertEquals("Acme Corp", result.get(0).getCompanyName());
        }

        @Test
        @DisplayName("should throw when user not found")
        void testGetAll_UserNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.getAllCustomersByUser(99));
        }

        @Test
        @DisplayName("should return empty list when no active customers")
        void testGetAll_EmptyList() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(customerRepository.findAllByUserAndIsActiveTrue(testUser)).thenReturn(List.of());

            List<CustomerDto> result = customerService.getAllCustomersByUser(1);
            assertTrue(result.isEmpty());
        }
    }

    // ─────────────────────── CREATE CUSTOMER ───────────────────────

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should create a new customer successfully")
        void testCreate_Success() {
            CustomerDto dto = buildDto();
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);

            CustomerDto result = customerService.createCustomer(1, dto);

            assertNotNull(result);
            assertEquals("Acme Corp", result.getCompanyName());
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw when user not found on create")
        void testCreate_UserNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.createCustomer(99, buildDto()));
        }
    }

    // ─────────────────────── UPDATE CUSTOMER ───────────────────────

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should update customer fields correctly")
        void testUpdate_Success() {
            CustomerDto dto = buildDto();
            dto.setCompanyName("New Corp");

            when(customerRepository.findById(10)).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setCompanyName("New Corp");
                return c;
            });

            CustomerDto result = customerService.updateCustomer(10, dto);
            assertEquals("New Corp", result.getCompanyName());
        }

        @Test
        @DisplayName("should throw when customer not found on update")
        void testUpdate_NotFound() {
            when(customerRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.updateCustomer(999, buildDto()));
        }
    }

    // ─────────────────────── SOFT DELETE CUSTOMER ───────────────────────

    @Nested
    @DisplayName("deleteCustomer")
    class DeleteCustomer {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should soft-delete a customer (set isActive=false)")
        void testDelete_Success() {
            when(customerRepository.findById(10)).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);

            customerService.deleteCustomer(10);

            assertFalse(activeCustomer.getIsActive());
            verify(customerRepository).save(activeCustomer);
        }

        @Test
        @DisplayName("should throw when customer not found on delete")
        void testDelete_NotFound() {
            when(customerRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.deleteCustomer(999));
        }
    }

    // ─────────────────────── PAY PENDING AMOUNT ───────────────────────

    @Nested
    @DisplayName("payCustomerPendingAmount")
    class PayPendingAmount {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should clear date-specific pending amount")
        void testPayPending_Success() {
            LocalDate dt = LocalDate.of(2026, 4, 1);
            activeCustomer.getPendingHistory().put(dt, new BigDecimal("500.00"));
            activeCustomer.setOutstandingBalance(new BigDecimal("500.00"));

            when(customerRepository.findById(10)).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);

            customerService.payCustomerPendingAmount(10, dt);

            assertEquals(BigDecimal.ZERO, activeCustomer.getOutstandingBalance());
            assertFalse(activeCustomer.getPendingHistory().containsKey(dt));
        }

        @Test
        @SuppressWarnings("null")
        @DisplayName("should not go negative on outstanding balance")
        void testPayPending_NoNegative() {
            LocalDate dt = LocalDate.of(2026, 4, 1);
            activeCustomer.getPendingHistory().put(dt, new BigDecimal("1000.00"));
            activeCustomer.setOutstandingBalance(new BigDecimal("100.00")); // less than pending entry

            when(customerRepository.findById(10)).thenReturn(Optional.of(activeCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);

            customerService.payCustomerPendingAmount(10, dt);

            assertEquals(0, activeCustomer.getOutstandingBalance().compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("should do nothing if date not in pending history")
        void testPayPending_DateNotFound() {
            when(customerRepository.findById(10)).thenReturn(Optional.of(activeCustomer));
            // No history set → nothing should happen, no save should occur
            customerService.payCustomerPendingAmount(10, LocalDate.now());
            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    // ─────────────────────── HELPERS ───────────────────────

    private CustomerDto buildDto() {
        CustomerDto dto = new CustomerDto();
        dto.setCompanyName("Acme Corp");
        dto.setCustomerName("John Doe");
        dto.setEmail("john@acme.com");
        dto.setContactNumber("9876543210");
        dto.setAddress("123 Market St");
        dto.setCity("Mumbai");
        dto.setState("Maharashtra");
        dto.setPinCode("400001");
        dto.setCustomerType("RETAIL");
        dto.setGstin("GST123456");
        return dto;
    }
}
