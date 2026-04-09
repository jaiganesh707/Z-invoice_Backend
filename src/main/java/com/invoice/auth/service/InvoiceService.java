package com.invoice.auth.service;

import com.invoice.auth.exception.ResourceNotFoundException;
import java.util.Objects;

import com.invoice.auth.dto.CreateInvoiceDto;
import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.InvoiceItem;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.FoodItemRepository;
import com.invoice.auth.repository.InvoiceRepository;
import com.invoice.auth.repository.UserRepository;
import com.invoice.auth.repository.CustomTaxRepository;
import com.invoice.auth.repository.CustomerRepository;
import com.invoice.auth.repository.EmployeeRepository;
import com.invoice.auth.entity.Customer;
import com.invoice.auth.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final CustomTaxRepository customTaxRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    private void addCustomerPendingAmount(Customer customer, BigDecimal invoiceTotal, BigDecimal invoicePaid) {
        if (customer == null)
            return;
        BigDecimal pending = invoiceTotal.subtract(invoicePaid != null ? invoicePaid : BigDecimal.ZERO);
        if (pending.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal current = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance()
                    : BigDecimal.ZERO;
            customer.setOutstandingBalance(current.add(pending));

            // Map store logic (Date wise)
            java.time.LocalDate today = java.time.LocalDate.now();
            if (customer.getPendingHistory() == null) {
                customer.setPendingHistory(new java.util.HashMap<>());
            }
            BigDecimal dailyPending = customer.getPendingHistory().getOrDefault(today, BigDecimal.ZERO);
            customer.getPendingHistory().put(today, dailyPending.add(pending));

            customerRepository.save(customer);
        }
    }

    @Transactional
    public Invoice createInvoice(CreateInvoiceDto dto) {
        Objects.requireNonNull(dto, "Invoice DTO must not be null");
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Invoice must contain at least one item");
        }

        @SuppressWarnings("null")
        User user = userRepository.findById(java.util.Objects.requireNonNull(dto.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

        List<InvoiceItem> invoiceItems = new java.util.ArrayList<>();
        BigDecimal totalAmount = java.math.BigDecimal.ZERO;

        Invoice invoice = new Invoice();
        invoice.setUser(user);

        // Set Customer if provided
        if (dto.getCustomerId() != null) {
            Customer customer = customerRepository.findById(Objects.requireNonNull(dto.getCustomerId())).orElse(null);
            invoice.setCustomer(customer);
        }

        // Generate Professional Invoice Number
        String invPrefix = "INV-"
                + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now()) + "-";
        invoice.setInvoiceNumber(invPrefix + String.format("%04d", (int) (Math.random() * 10000)));

        // Set Creator Identity
        if (dto.getCreatorId() != null) {
            User creator = userRepository.findById(Objects.requireNonNull(dto.getCreatorId())).orElse(null);
            if (creator != null) {
                invoice.setCreatedBy(creator);
                invoice.setCreatorName(creator.getUsername());
            } else {
                Employee emp = employeeRepository.findById(Objects.requireNonNull(dto.getCreatorId())).orElse(null);
                if (emp != null) {
                    invoice.setCreatorEmployeeId(emp.getId());
                    invoice.setCreatorName(emp.getUsername());
                }
            }
        }

        // Set Status logic: Default to 'CREATED' as requested
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            invoice.setStatus(dto.getStatus());
        } else {
            invoice.setStatus("CREATED");
        }

        // AUTO-AUTHORIZATION LOGIC: If the invoice was initiated by a Prime User
        // (the business owner/boss), it is considered pre-verified and crystallized.
        if (invoice.getCreatedBy() != null &&
                invoice.getCreatedBy().getRole() == com.invoice.auth.entity.RoleEnum.ROLE_PRIME_USER &&
                !"DRAFT".equals(invoice.getStatus())) {
            invoice.setStatus("APPROVED");
            invoice.setApprovedBy(invoice.getCreatedBy());
        }

        for (CreateInvoiceDto.InvoiceItemDto itemDto : dto.getItems()) {
            if (itemDto.getFoodItemId() == null || itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid item data: quantity must be positive and food item ID must not be null");
            }
            FoodItem foodItem = foodItemRepository.findById(Objects.requireNonNull(itemDto.getFoodItemId()))
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

        // Apply Custom Taxes
        List<com.invoice.auth.entity.CustomTax> activeTaxes = customTaxRepository.findAllByUserAndIsActiveTrue(user);
        BigDecimal totalTaxPercentage = activeTaxes.stream()
                .map(com.invoice.auth.entity.CustomTax::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = totalAmount.multiply(totalTaxPercentage).divide(new BigDecimal(100));
        BigDecimal finalTotalAmount = totalAmount.add(totalTax);

        // Balance & Payment Logic
        BigDecimal previousBalance = BigDecimal.ZERO;
        if (invoice.getCustomer() != null) {
            previousBalance = getCustomerBalance(invoice.getCustomer().getId());
            // Add existing customer outstanding balance to previousBalance if any
            if (invoice.getCustomer().getOutstandingBalance() != null) {
                previousBalance = previousBalance.add(invoice.getCustomer().getOutstandingBalance());
            }
        }

        BigDecimal paid = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = dto.getOutstandingAmount() != null ? dto.getOutstandingAmount() : BigDecimal.ZERO;
        invoice.setOutstandingAmount(outstanding);
        invoice.setDeliveryRequired(dto.isDeliveryRequired());
        if (dto.isDeliveryRequired()) {
            invoice.setDeliveryStatus("PENDING");
        }

        BigDecimal balance = finalTotalAmount.add(previousBalance).subtract(paid);

        invoice.setItems(invoiceItems);
        invoice.setTotalAmount(finalTotalAmount);
        invoice.setPreviousBalance(previousBalance);
        invoice.setPaidAmount(paid);
        invoice.setBalanceAmount(balance);
        invoice.setBillingAddress(dto.getBillingAddress());
        invoice.setCustomerGstin(dto.getCustomerGstin());

        if ("APPROVED".equals(invoice.getStatus())) {
            addCustomerPendingAmount(invoice.getCustomer(), finalTotalAmount, paid);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getPendingInvoices(User approver) {
        if (approver == null)
            throw new IllegalArgumentException("Approver must not be null");

        // Approver sees invoices for their parent user (Prime)
        User targetUser = approver.getParentUser() != null ? approver.getParentUser() : approver;

        List<Invoice> invoices = invoiceRepository.findAllByBusinessGroupId(targetUser.getId());

        // Filter in memory for robust status-aware verification queue
        List<String> targetStatuses = java.util.Arrays.asList("CREATED", "PENDING", "PENDING_APPROVAL");
        invoices = invoices.stream()
                .filter(i -> targetStatuses.contains(i.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        invoices.forEach(i -> {
            if (i.getItems() != null)
                i.getItems().size();
        });
        return invoices;
    }

    @Transactional
    public Invoice approveInvoice(Integer invoiceId, Integer approverId, boolean addOutstandingToTotal,
            BigDecimal paidAmount) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User approver = approverId != null ? userRepository.findById(approverId).orElse(null) : null;

        if (paidAmount != null) {
            invoice.setPaidAmount(paidAmount);
        }

        if (addOutstandingToTotal && invoice.getOutstandingAmount() != null
                && invoice.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal legacyDebt = invoice.getOutstandingAmount();
            invoice.setPreviousBalance(legacyDebt); // Explicitly track for forensic itemization
            invoice.setTotalAmount(invoice.getTotalAmount().add(legacyDebt));
            invoice.setOutstandingAmount(BigDecimal.ZERO);

            // AUTO-CLEAR PENDING HISTORY MAP: Everything is now rolled into this new bill
            if (invoice.getCustomer() != null) {
                Customer customer = invoice.getCustomer();
                if (customer.getPendingHistory() != null) {
                    customer.getPendingHistory().clear();
                }
                customer.setOutstandingBalance(BigDecimal.ZERO);
                customerRepository.save(customer);
            }
        }

        // CALCULATE FINAL STATEMENT BALANCE
        // If we added outstanding to total, then invoice.getTotalAmount() already
        // contains previousBalance.
        // Otherwise, we keep them separate in the statement's balance calculation to
        // maintain forensic integrity.
        if (addOutstandingToTotal) {
            invoice.setBalanceAmount(invoice.getTotalAmount().subtract(invoice.getPaidAmount()));
        } else {
            // If not rolled in, this specific invoice's balance is just its own total minus
            // what was paid towards it.
            // The customer's total liability still includes previousBalance in the ledger
            // overall.
            invoice.setBalanceAmount(invoice.getTotalAmount().subtract(invoice.getPaidAmount()));
        }

        invoice.setStatus("APPROVED");
        invoice.setApprovedBy(approver);

        addCustomerPendingAmount(invoice.getCustomer(), invoice.getTotalAmount(), invoice.getPaidAmount());

        // Initialize for downstream view
        if (invoice.getItems() != null)
            invoice.getItems().size();
        if (invoice.getCustomer() != null) {
            invoice.getCustomer().getCompanyName();
            if (invoice.getCustomer().getPendingHistory() != null)
                invoice.getCustomer().getPendingHistory().size();
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCustomerBalance(Integer customerId) {
        Customer customer = customerRepository.findById(Objects.requireNonNull(customerId)).orElse(null);
        return customer != null && customer.getOutstandingBalance() != null ? customer.getOutstandingBalance()
                : BigDecimal.ZERO;
    }

    @Transactional
    public Invoice updatePayment(Integer invoiceId, BigDecimal incrementPaid) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (incrementPaid == null || incrementPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return invoice;
        }

        BigDecimal newPaidAmount = (invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO)
                .add(incrementPaid);
        invoice.setPaidAmount(newPaidAmount);

        // Re-calculate balance
        BigDecimal totalLiability = (invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                .add(invoice.getPreviousBalance() != null ? invoice.getPreviousBalance() : BigDecimal.ZERO);

        invoice.setBalanceAmount(totalLiability.subtract(newPaidAmount));

        // Sync with Customer Ledger: Subtract the payment from their total debt
        if (invoice.getCustomer() != null) {
            Customer customer = invoice.getCustomer();
            BigDecimal currentOutstanding = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance()
                    : BigDecimal.ZERO;
            customer.setOutstandingBalance(currentOutstanding.subtract(incrementPaid));
            customerRepository.save(customer);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice submitForApproval(Integer invoiceId, String submissionNote) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only DRAFT invoices can be submitted for approval. Current status: " + invoice.getStatus());
        }

        invoice.setStatus("CREATED");
        if (submissionNote != null && !submissionNote.isBlank()) {
            invoice.setSubmissionNote(submissionNote);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice rejectInvoice(Integer invoiceId, String reason) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setStatus("REJECTED");
        invoice.setRejectionReason(reason);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markAsPending(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setStatus("PENDING");
        invoice.setRejectionReason(null); // Clear any rejection reason

        // New requirement: Add amount to customer balance when marked as pending
        if (invoice.getCustomer() != null) {
            addCustomerPendingAmount(invoice.getCustomer(), invoice.getTotalAmount(), invoice.getPaidAmount());
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice assignDriverToInvoice(Integer invoiceId, Integer driverUserId) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        User driver = userRepository.findById(Objects.requireNonNull(driverUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found"));

        if (driver.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_DRIVER) {
            throw new IllegalArgumentException("User is not a driver");
        }

        invoice.setAssignedDriver(driver);
        invoice.setDeliveryStatus("ASSIGNED");

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateDeliveryStatus(Integer invoiceId, String status, BigDecimal amountCollected) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        invoice.setDeliveryStatus(status);
        if (amountCollected != null) {
            invoice.setAmountCollectedByDriver(amountCollected);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice settleDelivery(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(Objects.requireNonNull(invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!"DELIVERED".equals(invoice.getDeliveryStatus())) {
            throw new IllegalStateException("Invoice delivery is not completed yet");
        }

        BigDecimal shortfall = invoice.getTotalAmount().subtract(invoice.getAmountCollectedByDriver());
        if (shortfall.compareTo(BigDecimal.ZERO) > 0 && invoice.getCustomer() != null) {
            Customer customer = invoice.getCustomer();
            BigDecimal currentOutstanding = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance()
                    : BigDecimal.ZERO;
            customer.setOutstandingBalance(currentOutstanding.add(shortfall));
            customerRepository.save(customer);
        }

        invoice.setStatus("SETTLED");
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
        java.util.Optional<com.invoice.auth.entity.User> userOpt = userRepository.findById((int) userId);
        if (userOpt.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        com.invoice.auth.entity.User user = userOpt.get();
        boolean isPrivileged = user.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_PRIME_USER ||
                user.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_APPROVER ||
                user.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_WORKFLOW_USER;

        // Ensure we load the global business identity for workers
        User primeUser = (user.getParentUser() != null) ? user.getParentUser() : user;

        List<Invoice> invoices;

        // Final fallback: if worker has no parent linked, they see only their own.
        // But if they have a parent, they see the whole group.

        if (start != null && end != null) {
            if (user.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_DRIVER) {
                invoices = invoiceRepository.findAllByAssignedDriverAndCreatedAtBetweenOrderByCreatedAtDesc(user, start,
                        end);
            } else if (isPrivileged) {
                invoices = invoiceRepository.findAllByBusinessGroupIdAndDateRange(primeUser.getId(), start, end);
            } else {
                invoices = invoiceRepository.findAllByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, start, end);
            }
        } else {
            if (user.getRole() == com.invoice.auth.entity.RoleEnum.ROLE_DRIVER) {
                invoices = invoiceRepository.findAllByAssignedDriver(user);
            } else if (isPrivileged) {
                invoices = invoiceRepository.findAllByBusinessGroupId(primeUser.getId());
            } else {
                invoices = invoiceRepository.findAllByUserOrderByCreatedAtDesc(user);
            }
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
            if (i.getCreatedBy() != null) {
                i.getCreatedBy().getUsername(); // Touch the creator
            }
            if (i.getApprovedBy() != null) {
                i.getApprovedBy().getUsername(); // Touch the final verifier (approver)
            }
        });

        return invoices;
    }

    @Transactional
    public void deleteInvoice(Integer id) {
        Objects.requireNonNull(id, "Invoice ID must not be null");
        if (!invoiceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Invoice not found with id: " + id);
        }
        invoiceRepository.deleteById(id);
    }
}
