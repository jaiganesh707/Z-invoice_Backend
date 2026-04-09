package com.invoice.auth.service;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.*;
import com.invoice.auth.exception.ResourceNotFoundException;
import com.invoice.auth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("InvoiceService Unit Tests — Full Coverage Suite")
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomTaxRepository customTaxRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User primeUser;
    private User workflowUser;
    private User driverUser;
    private FoodItem sampleProduct;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        primeUser = new User();
        primeUser.setId(1);
        primeUser.setUsername("prime");
        primeUser.setRole(RoleEnum.ROLE_PRIME_USER);

        workflowUser = new User();
        workflowUser.setId(2);
        workflowUser.setUsername("employee1");
        workflowUser.setRole(RoleEnum.ROLE_WORKFLOW_USER);
        workflowUser.setParentUser(primeUser);

        driverUser = new User();
        driverUser.setId(3);
        driverUser.setUsername("driver1");
        driverUser.setRole(RoleEnum.ROLE_DRIVER);

        sampleProduct = FoodItem.builder()
                .id(100)
                .name("Biryani")
                .price(new BigDecimal("150.00"))
                .isActive(true)
                .uniqueCode("ITM-BIRY0001")
                .build();

        sampleCustomer = Customer.builder()
                .id(200)
                .companyName("Elite Eats")
                .customerName("Raj Kumar")
                .outstandingBalance(BigDecimal.ZERO)
                .pendingHistory(new java.util.HashMap<>())
                .isActive(true)
                .build();
    }

    // ─────────────────────── CREATE INVOICE ───────────────────────

    @Nested
    @DisplayName("createInvoice")
    class CreateInvoice {

        @Test
        @DisplayName("should create invoice with CREATED status for workflow user")
        void testCreate_WorkflowUser_CreatedStatus() {
            int workflowUserId = Objects.requireNonNull(workflowUser.getId());
            CreateInvoiceDto dto = buildDto(workflowUserId, null, null);

            when(userRepository.findById(workflowUserId)).thenReturn(Optional.of(workflowUser));
            when(foodItemRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
            when(customTaxRepository.findAllByUserAndIsActiveTrue(any())).thenReturn(new ArrayList<>());
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.createInvoice(dto);

            assertEquals("CREATED", result.getStatus());
            assertNotNull(result);
            verify(invoiceRepository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("should AUTO-APPROVE invoice when creator is Prime User")
        void testCreate_PrimeUser_AutoApproved() {
            int primeUserId = Objects.requireNonNull(primeUser.getId());
            CreateInvoiceDto dto = buildDto(primeUserId, primeUserId, null);

            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(foodItemRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
            when(customTaxRepository.findAllByUserAndIsActiveTrue(any())).thenReturn(new ArrayList<>());
            when(customerRepository.findById(any())).thenReturn(Optional.of(sampleCustomer));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.createInvoice(dto);

            assertEquals("APPROVED", result.getStatus());
            assertEquals(primeUser, result.getApprovedBy());
        }

        @Test
        @DisplayName("should keep DRAFT status when explicitly set")
        void testCreate_DraftStatus() {
            int primeUserId = Objects.requireNonNull(primeUser.getId());
            CreateInvoiceDto dto = buildDto(primeUserId, primeUserId, null);
            dto.setStatus("DRAFT");

            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(foodItemRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
            when(customTaxRepository.findAllByUserAndIsActiveTrue(any())).thenReturn(new ArrayList<>());
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.createInvoice(dto);

            assertEquals("DRAFT", result.getStatus());
        }

        @Test
        @DisplayName("should apply tax correctly to total amount")
        void testCreate_TaxCalculation() {
            int primeUserId = Objects.requireNonNull(primeUser.getId());
            CreateInvoiceDto dto = buildDto(primeUserId, null, null);

            // 10% tax
            CustomTax tax10 = new CustomTax();
            tax10.setPercentage(new BigDecimal("10"));

            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(foodItemRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
            when(customTaxRepository.findAllByUserAndIsActiveTrue(any())).thenReturn(List.of(tax10));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.createInvoice(dto);

            // 1 unit of Biryani @ 150 + 10% = 165
            assertEquals(new BigDecimal("165.00"), result.getTotalAmount().setScale(2));
        }

        @Test
        @DisplayName("should set balance = total - paid")
        void testCreate_BalanceCalculation() {
            int primeUserId = Objects.requireNonNull(primeUser.getId());
            CreateInvoiceDto dto = buildDto(primeUserId, null, null);
            dto.setPaidAmount(new BigDecimal("50.00"));

            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(foodItemRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
            when(customTaxRepository.findAllByUserAndIsActiveTrue(any())).thenReturn(new ArrayList<>());
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.createInvoice(dto);

            // 150 - 50 = 100
            assertEquals(0, new BigDecimal("100.00").compareTo(result.getBalanceAmount()));
        }

        @Test
        @DisplayName("should throw if userId is null")
        void testCreate_NullUserId() {
            CreateInvoiceDto dto = new CreateInvoiceDto();
            dto.setUserId(null);
            dto.setItems(List.of(new CreateInvoiceDto.InvoiceItemDto()));

            assertThrows(Exception.class, () -> invoiceService.createInvoice(dto));
        }

        @Test
        @DisplayName("should throw if items list is empty")
        void testCreate_EmptyItems() {
            CreateInvoiceDto dto = new CreateInvoiceDto();
            dto.setUserId(primeUser.getId());
            dto.setItems(new ArrayList<>());

            assertThrows(IllegalArgumentException.class, () -> invoiceService.createInvoice(dto));
        }

        @Test
        @DisplayName("should throw if user not found")
        void testCreate_UserNotFound() {
            CreateInvoiceDto dto = buildDto(999, null, null);
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> invoiceService.createInvoice(dto));
        }
    }

    // ─────────────────────── APPROVE INVOICE ───────────────────────

    @Nested
    @DisplayName("approveInvoice")
    class ApproveInvoice {

        @Test
        @DisplayName("should approve with full payment (settled)")
        void testApprove_FullPayment() {
            Invoice invoice = buildPendingInvoice(1, new BigDecimal("300.00"));

            int primeUserId = Objects.requireNonNull(primeUser.getId());
            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.approveInvoice(1, primeUserId, false, new BigDecimal("300.00"));

            assertEquals("APPROVED", result.getStatus());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalanceAmount()));
        }

        @Test
        @DisplayName("should track partial payment as balance")
        void testApprove_PartialPayment() {
            Invoice invoice = buildPendingInvoice(2, new BigDecimal("200.00"));

            int primeUserId = Objects.requireNonNull(primeUser.getId());
            when(invoiceRepository.findById(2)).thenReturn(Optional.of(invoice));
            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.approveInvoice(2, primeUserId, false, new BigDecimal("100.00"));

            assertEquals("APPROVED", result.getStatus());
            assertEquals(0, new BigDecimal("100.00").compareTo(result.getBalanceAmount()));
        }

        @Test
        @DisplayName("should roll in outstanding amount when flag is set")
        void testApprove_WithOutstandingRollIn() {
            Invoice invoice = buildPendingInvoice(3, new BigDecimal("200.00"));
            invoice.setOutstandingAmount(new BigDecimal("50.00"));
            invoice.setCustomer(sampleCustomer);
            sampleCustomer.setPendingHistory(new java.util.HashMap<>());

            int primeUserId = Objects.requireNonNull(primeUser.getId());
            when(invoiceRepository.findById(3)).thenReturn(Optional.of(invoice));
            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.approveInvoice(3, primeUserId, true, new BigDecimal("250.00"));

            // total becomes 250, paid = 250, balance = 0
            assertEquals("APPROVED", result.getStatus());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getOutstandingAmount()));
        }

        @Test
        @DisplayName("should throw when invoice not found")
        void testApprove_NotFound() {
            when(invoiceRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.approveInvoice(999, 1, false, BigDecimal.ZERO));
        }
    }

    // ─────────────────────── SUBMIT FOR APPROVAL ───────────────────────

    @Nested
    @DisplayName("submitForApproval")
    class SubmitForApproval {

        @Test
        @DisplayName("should transition DRAFT → CREATED")
        void testSubmit_Success() {
            Invoice invoice = buildPendingInvoice(1, BigDecimal.TEN);
            invoice.setStatus("DRAFT");

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.submitForApproval(1, "Please review");

            assertEquals("CREATED", result.getStatus());
            assertEquals("Please review", result.getSubmissionNote());
        }

        @Test
        @DisplayName("should throw when invoice is not in DRAFT state")
        void testSubmit_NotDraft() {
            Invoice invoice = buildPendingInvoice(2, BigDecimal.TEN);
            invoice.setStatus("CREATED");

            when(invoiceRepository.findById(2)).thenReturn(Optional.of(invoice));

            assertThrows(IllegalStateException.class,
                    () -> invoiceService.submitForApproval(2, "note"));
        }

        @Test
        @DisplayName("should throw when invoice not found")
        void testSubmit_NotFound() {
            when(invoiceRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.submitForApproval(999, "note"));
        }
    }

    // ─────────────────────── REJECT INVOICE ───────────────────────

    @Nested
    @DisplayName("rejectInvoice")
    class RejectInvoice {

        @Test
        @DisplayName("should set status to REJECTED with reason")
        void testReject_Success() {
            Invoice invoice = buildPendingInvoice(1, BigDecimal.TEN);

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.rejectInvoice(1, "Duplicate entry");

            assertEquals("REJECTED", result.getStatus());
            assertEquals("Duplicate entry", result.getRejectionReason());
        }
    }

    // ─────────────────────── UPDATE PAYMENT ───────────────────────

    @Nested
    @DisplayName("updatePayment")
    class UpdatePayment {

        @Test
        @DisplayName("should increment paid amount and reduce balance")
        void testUpdatePayment_Success() {
            Invoice invoice = buildPendingInvoice(1, new BigDecimal("200.00"));
            invoice.setPaidAmount(new BigDecimal("50.00"));
            invoice.setBalanceAmount(new BigDecimal("150.00"));

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.updatePayment(1, new BigDecimal("50.00"));

            assertEquals(0, new BigDecimal("100.00").compareTo(result.getPaidAmount()));
            assertEquals(0, new BigDecimal("100.00").compareTo(result.getBalanceAmount()));
        }

        @Test
        @DisplayName("should sync customer outstanding balance")
        void testUpdatePayment_CustomerSync() {
            Invoice invoice = buildPendingInvoice(2, new BigDecimal("300.00"));
            invoice.setCustomer(sampleCustomer);
            sampleCustomer.setOutstandingBalance(new BigDecimal("300.00"));
            invoice.setPaidAmount(BigDecimal.ZERO);

            when(invoiceRepository.findById(2)).thenReturn(Optional.of(invoice));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            invoiceService.updatePayment(2, new BigDecimal("100.00"));

            // Customer outstanding should have been reduced by 100
            assertEquals(0, new BigDecimal("200.00").compareTo(sampleCustomer.getOutstandingBalance()));
        }

        @Test
        @DisplayName("should ignore zero or negative increment")
        void testUpdatePayment_ZeroIncrement() {
            Invoice invoice = buildPendingInvoice(3, new BigDecimal("100.00"));

            when(invoiceRepository.findById(3)).thenReturn(Optional.of(invoice));

            Invoice result = invoiceService.updatePayment(3, BigDecimal.ZERO);

            verify(invoiceRepository, never()).save(any());
        }
    }

    // ─────────────────────── ASSIGN DRIVER ───────────────────────

    @Nested
    @DisplayName("assignDriverToInvoice")
    class AssignDriver {

        @Test
        @DisplayName("should assign a driver role user to invoice")
        void testAssign_Success() {
            Invoice invoice = buildPendingInvoice(1, BigDecimal.TEN);

            int driverUserId = Objects.requireNonNull(driverUser.getId());
            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(userRepository.findById(driverUserId)).thenReturn(Optional.of(driverUser));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.assignDriverToInvoice(1, driverUserId);

            assertEquals(driverUser, result.getAssignedDriver());
            assertEquals("ASSIGNED", result.getDeliveryStatus());
        }

        @Test
        @DisplayName("should throw when user assigned is not a driver")
        void testAssign_NotDriver() {
            Invoice invoice = buildPendingInvoice(1, BigDecimal.TEN);

            int primeUserId = Objects.requireNonNull(primeUser.getId());
            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(userRepository.findById(primeUserId)).thenReturn(Optional.of(primeUser));

            assertThrows(IllegalArgumentException.class,
                    () -> invoiceService.assignDriverToInvoice(1, primeUserId));
        }
    }

    // ─────────────────────── DELETE INVOICE ───────────────────────

    @Nested
    @DisplayName("deleteInvoice")
    class DeleteInvoice {

        @Test
        @DisplayName("should delete existing invoice")
        void testDelete_Success() {
            when(invoiceRepository.existsById(1)).thenReturn(true);
            doNothing().when(invoiceRepository).deleteById(1);

            assertDoesNotThrow(() -> invoiceService.deleteInvoice(1));
            verify(invoiceRepository).deleteById(1);
        }

        @Test
        @DisplayName("should throw when invoice not found for deletion")
        void testDelete_NotFound() {
            when(invoiceRepository.existsById(999)).thenReturn(false);
            assertThrows(ResourceNotFoundException.class, () -> invoiceService.deleteInvoice(999));
        }

        @Test
        @DisplayName("should throw when invoice id is null")
        void testDelete_NullId() {
            assertThrows(NullPointerException.class, () -> invoiceService.deleteInvoice(null));
        }
    }

    // ─────────────────────── SETTLE DELIVERY ───────────────────────

    @Nested
    @DisplayName("settleDelivery")
    class SettleDelivery {

        @Test
        @DisplayName("should set SETTLED status when delivery is DELIVERED")
        void testSettle_Success() {
            Invoice invoice = buildPendingInvoice(1, new BigDecimal("200.00"));
            invoice.setDeliveryStatus("DELIVERED");
            invoice.setAmountCollectedByDriver(new BigDecimal("200.00"));

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            Invoice result = invoiceService.settleDelivery(1);
            assertEquals("SETTLED", result.getStatus());
        }

        @Test
        @DisplayName("should throw if delivery is not yet DELIVERED")
        void testSettle_NotDelivered() {
            Invoice invoice = buildPendingInvoice(1, BigDecimal.TEN);
            invoice.setDeliveryStatus("IN_TRANSIT");

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));

            assertThrows(IllegalStateException.class, () -> invoiceService.settleDelivery(1));
        }

        @Test
        @DisplayName("should add shortfall to customer outstanding when driver undersettled")
        void testSettle_ShortfallToCustomer() {
            Invoice invoice = buildPendingInvoice(1, new BigDecimal("300.00"));
            invoice.setDeliveryStatus("DELIVERED");
            invoice.setAmountCollectedByDriver(new BigDecimal("200.00")); // shortfall = 100
            invoice.setCustomer(sampleCustomer);

            when(invoiceRepository.findById(1)).thenReturn(Optional.of(invoice));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

            invoiceService.settleDelivery(1);

            // shortfall = 100 added to customer outstanding
            assertEquals(0, new BigDecimal("100.00").compareTo(sampleCustomer.getOutstandingBalance()));
        }
    }

    // ─────────────────────── HELPERS ───────────────────────

    private CreateInvoiceDto buildDto(Integer userId, Integer creatorId, Integer customerId) {
        CreateInvoiceDto dto = new CreateInvoiceDto();
        dto.setUserId(userId);
        dto.setCreatorId(creatorId);
        dto.setCustomerId(customerId);

        CreateInvoiceDto.InvoiceItemDto itemDto = new CreateInvoiceDto.InvoiceItemDto();
        itemDto.setFoodItemId(100);
        itemDto.setQuantity(1);
        dto.setItems(List.of(itemDto));
        return dto;
    }

    private Invoice buildPendingInvoice(Integer id, BigDecimal total) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setStatus("CREATED");
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceAmount(total);
        invoice.setOutstandingAmount(BigDecimal.ZERO);
        invoice.setPreviousBalance(BigDecimal.ZERO);
        invoice.setAmountCollectedByDriver(BigDecimal.ZERO);
        return invoice;
    }
}
